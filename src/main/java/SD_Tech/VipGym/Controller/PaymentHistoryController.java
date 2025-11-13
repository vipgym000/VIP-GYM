package SD_Tech.VipGym.Controller;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SD_Tech.VipGym.Dto.PaymentHistoryDTO;
import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Entity.User;
import SD_Tech.VipGym.Repository.PaymentRepository;
import SD_Tech.VipGym.Repository.UserRepository;
import SD_Tech.VipGym.Service.GoogleDriveUploadService; // ✅ new Drive service
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@RestController
@RequestMapping("/api/payments")
public class PaymentHistoryController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GoogleDriveUploadService googleDriveUploadService; // ✅ replacing GitHub service

    @PersistenceContext
    private EntityManager entityManager;

    // ✅ Get all payment records with user details (DTO-based)
    @GetMapping("/history/all")
    @Transactional
    public ResponseEntity<?> getAllPaymentHistory() {
        try {
            List<Payment> payments = paymentRepository.findAllWithUser();

            List<Map<String, Object>> result = payments.stream().map(p -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", p.getId());
                map.put("amount", p.getAmount());
                map.put("paymentDate", p.getPaymentDate());
                map.put("paymentMethod", p.getPaymentMethod());
                map.put("receiptUrl", p.getReceiptUrl());
                map.put("remarks", p.getRemarks());

                if (p.getUser() != null) {
                    map.put("userId", p.getUser().getId());
                    map.put("userFullName", p.getUser().getFullName());
                    map.put("userEmail", p.getUser().getEmail());
                    map.put("userMobileNumber", p.getUser().getMobileNumber());
                    map.put("userProfilePictureUrl", p.getUser().getProfilePictureUrl());
                }

                return map;
            }).toList();

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch payment history: " + e.getMessage()));
        }
    }

    // ✅ Get payment history for a specific user
    @GetMapping("/history/user/{userId}")
    @Transactional
    public ResponseEntity<?> getUserPaymentHistory(@PathVariable Long userId) {
        try {
            if (!userRepository.existsById(userId)) {
                return ResponseEntity.status(404).body("User not found with ID: " + userId);
            }

            EntityGraph<?> graph = entityManager.createEntityGraph(Payment.class);
            graph.addAttributeNodes("id", "paymentDate", "amount", "paymentMethod", "receiptUrl",
                    "paidAmount", "pendingAmount", "nextDueDate");
            graph.addSubgraph("user").addAttributeNodes("id", "fullName", "email", "mobileNumber", "profilePictureUrl");

            List<Payment> payments = entityManager
                    .createQuery("SELECT p FROM Payment p WHERE p.user.id = :userId ORDER BY p.paymentDate DESC",
                            Payment.class)
                    .setParameter("userId", userId)
                    .setHint("jakarta.persistence.fetchgraph", graph)
                    .getResultList();

            List<PaymentHistoryDTO> paymentDTOs = payments.stream().map(p -> new PaymentHistoryDTO(
                    p.getId(),
                    p.getPaymentDate(),
                    p.getAmount(),
                    p.getPaymentMethod(),
                    p.getReceiptUrl(),
                    p.getUser() != null ? p.getUser().getFullName() : null,
                    p.getUser() != null ? p.getUser().getEmail() : null,
                    p.getUser() != null ? p.getUser().getProfilePictureUrl() : null
            )).collect(Collectors.toList());

            return ResponseEntity.ok(paymentDTOs);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to fetch payment history: " + e.getMessage());
        }
    }

    // ✅ Delete a single payment record (Drive-safe)
    @DeleteMapping("/history/{paymentId}")
    @Transactional
    public ResponseEntity<?> deletePayment(@PathVariable Long paymentId) {
        try {
            Payment payment = paymentRepository.findById(paymentId).orElse(null);
            if (payment == null) {
                return ResponseEntity.status(404).body("Payment not found with ID: " + paymentId);
            }

            User user = payment.getUser();

            if (user != null && user.getPayments() != null) {
                user.getPayments().remove(payment);
                userRepository.save(user);
            }

            // ✅ Delete from Google Drive
            if (payment.getReceiptUrl() != null && !payment.getReceiptUrl().isBlank()) {
                try {
                    String fileId = extractFileIdFromUrl(payment.getReceiptUrl());
                    if (fileId != null) {
                        googleDriveUploadService.deleteFile(fileId);
                        System.out.println("✅ Deleted Drive file: " + fileId);
                    }
                } catch (Exception ex) {
                    System.err.println("⚠️ Failed to delete Drive file for payment " + paymentId + ": " + ex.getMessage());
                }
            }

            paymentRepository.delete(payment);
            return ResponseEntity.ok("✅ Payment deleted successfully (ID: " + paymentId + ").");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("❌ Failed to delete payment with ID: " + paymentId + " - " + e.getMessage());
        }
    }

    // ✅ Helper method to extract Drive file ID from URL
    private String extractFileIdFromUrl(String url) {
        if (url == null || !url.contains("id=")) return null;
        return url.substring(url.indexOf("id=") + 3);
    }

    // ✅ Delete all payment records (and Drive receipts)
    @DeleteMapping("/history/all")
    @Transactional
    public ResponseEntity<?> deleteAllPayments() {
        try {
            List<Payment> payments = paymentRepository.findAll();
            if (payments.isEmpty()) {
                return ResponseEntity.ok("ℹ️ No payment records found to delete.");
            }

            int deletedReceipts = 0;

            for (Payment payment : payments) {
                String receiptUrl = payment.getReceiptUrl();
                if (receiptUrl != null && !receiptUrl.isBlank()) {
                    try {
                        String fileId = extractFileIdFromUrl(receiptUrl);
                        if (fileId != null) {
                            googleDriveUploadService.deleteFile(fileId);
                            deletedReceipts++;
                        }
                    } catch (Exception ex) {
                        System.err.println("⚠️ Error deleting Drive receipt for payment ID " + payment.getId() + ": " + ex.getMessage());
                    }
                }
            }

            // Unlink and delete
            userRepository.findAll().forEach(user -> {
                if (user.getPayments() != null) {
                    user.getPayments().clear();
                    userRepository.save(user);
                }
            });

            paymentRepository.deleteAll();

            return ResponseEntity.ok(
                    String.format("✅ Deleted %d payments and %d Drive receipts successfully.", payments.size(), deletedReceipts)
            );

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("❌ Failed to delete all payments: " + e.getMessage());
        }
    }

    // ✅ Export payment history as CSV
    @GetMapping("/history/export/csv")
    @Transactional
    public ResponseEntity<?> exportPaymentHistoryAsCSV() {
        try {
            EntityGraph<?> graph = entityManager.createEntityGraph(Payment.class);
            graph.addAttributeNodes("id", "paymentDate", "amount", "paymentMethod", "receiptUrl",
                    "paidAmount", "pendingAmount", "nextDueDate");
            graph.addSubgraph("user").addAttributeNodes("id", "fullName", "email", "mobileNumber");

            List<Payment> payments = entityManager
                    .createQuery("SELECT p FROM Payment p ORDER BY p.paymentDate DESC", Payment.class)
                    .setHint("jakarta.persistence.fetchgraph", graph)
                    .getResultList();

            StringBuilder csvContent = new StringBuilder();
            csvContent.append("ID,Payment Date,Amount,Payment Method,Receipt URL,User Name,User Email,User Mobile,Next Due Date\n");

            for (Payment payment : payments) {
                csvContent.append(payment.getId()).append(",");
                csvContent.append(payment.getPaymentDate()).append(",");
                csvContent.append(payment.getAmount()).append(",");
                csvContent.append(payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "N/A").append(",");
                csvContent.append(payment.getReceiptUrl() != null ? payment.getReceiptUrl() : "N/A").append(",");
                csvContent.append(payment.getUser() != null ? payment.getUser().getFullName() : "N/A").append(",");
                csvContent.append(payment.getUser() != null ? payment.getUser().getEmail() : "N/A").append(",");
                csvContent.append(payment.getUser() != null ? payment.getUser().getMobileNumber() : "N/A").append(",");
                csvContent.append(payment.getNextDueDate() != null ? payment.getNextDueDate() : "N/A").append("\n");
            }

            byte[] csvBytes = csvContent.toString().getBytes(StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payment_history.csv\"")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(csvBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to export payment history: " + e.getMessage());
        }
    }
}
