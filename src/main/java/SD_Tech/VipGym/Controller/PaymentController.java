package SD_Tech.VipGym.Controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Entity.User;
import SD_Tech.VipGym.Repository.PaymentRepository;
import SD_Tech.VipGym.Repository.UserRepository;
import SD_Tech.VipGym.Service.ByteArrayMultipartFile;
import SD_Tech.VipGym.Service.GitHubImageUploadService;
import SD_Tech.VipGym.Service.PaymentService;
import SD_Tech.VipGym.Service.ReceiptImageService;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReceiptImageService receiptImageService;

    @Autowired
    private GitHubImageUploadService gitHubImageUploadService;

    @PersistenceContext
    private EntityManager entityManager;

    // ✅ Get all payments for a specific user
    @GetMapping("/user/{userId}")
    @Transactional
    public ResponseEntity<?> getUserPayments(@PathVariable Long userId) {
        try {
            // Check user existence
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found with ID: " + userId));
            }

            // Fetch payments (EAGER fetch graph for user info)
            EntityGraph<?> graph = entityManager.createEntityGraph(Payment.class);
            graph.addAttributeNodes("id", "paymentDate", "amount", "paymentMethod", "receiptUrl", "paidAmount", "pendingAmount", "nextDueDate");
            graph.addSubgraph("user").addAttributeNodes("id", "fullName", "email", "pendingAmount", "totalPaid");

            List<Payment> payments = entityManager.createQuery(
                    "SELECT p FROM Payment p WHERE p.user.id = :userId ORDER BY p.paymentDate DESC", Payment.class)
                    .setParameter("userId", userId)
                    .setHint("jakarta.persistence.fetchgraph", graph)
                    .getResultList();

            Integer daysLeft = paymentService.getDaysUntilPlanExpires(payments);

            // ✅ Return user + payments + days left
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("payments", payments);
            response.put("daysLeft", daysLeft);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch payments: " + e.getMessage()));
        }
    }

    // ✅ Mark a new payment (includes receipt upload)
    @PostMapping("/mark")
    @Transactional
    public ResponseEntity<?> markPayment(@RequestBody Map<String, Object> paymentData) {
        try {
            Long userId = Long.valueOf(paymentData.get("userId").toString());
            Double amount = Double.valueOf(paymentData.get("amount").toString());
            String paymentMethod = paymentData.getOrDefault("paymentMethod", "CASH").toString();
            String remarks = paymentData.getOrDefault("remarks", "").toString();

            LocalDate nextDueDate = null;
            if (paymentData.get("nextDueDate") != null && !paymentData.get("nextDueDate").toString().isEmpty()) {
                nextDueDate = LocalDate.parse(paymentData.get("nextDueDate").toString());
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body(Map.of("message", "User not found with ID: " + userId));
            }

            // ✅ Calculate payment totals
            double newPaidAmount = user.getTotalPaid() + amount;
            double newPendingAmount = Math.max(user.getPendingAmount() - amount, 0.0);

            // ✅ Create payment record
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setPaymentDate(LocalDate.now());
            payment.setAmount(amount);
            payment.setPaymentMethod(paymentMethod);
            payment.setRemarks(remarks);
            payment.setPaidAmount(newPaidAmount);
            payment.setPendingAmount(newPendingAmount);

            // ✅ Handle next due date logic
            if (nextDueDate != null) {
                // If user explicitly provided new due date → use it
                payment.setNextDueDate(nextDueDate);
                user.setNextDueDate(nextDueDate);
            } else {
                // If user didn’t change due date, keep the same one
                if (newPendingAmount == 0) {
                    // ✅ Payment fully clears pending amount — keep same due date
                    payment.setNextDueDate(user.getNextDueDate());
                } else if (user.getMembership() != null && user.getNextDueDate() == null) {
                    // If user never had due date, calculate new
                    LocalDate calculatedDueDate = LocalDate.now().plusMonths(user.getMembership().getDurationInMonths());
                    payment.setNextDueDate(calculatedDueDate);
                    user.setNextDueDate(calculatedDueDate);
                } else {
                    // Default: keep current due date unchanged
                    payment.setNextDueDate(user.getNextDueDate());
                }
            }

            // ✅ Generate receipt
            byte[] receiptImage = receiptImageService.generateReceiptImage(
                    user.getFullName(),
                    user.getEmail(),
                    payment.getPaymentDate(),
                    amount,
                    user.getMembership() != null ? user.getMembership().getName() : "N/A",
                    paymentMethod
            );

            String receiptFileName = "receipt_" + user.getId() + "_" + System.currentTimeMillis() + ".png";
            MultipartFile multipartFile = new ByteArrayMultipartFile(receiptImage, receiptFileName, "image/png");
            String uploadedUrl = gitHubImageUploadService.uploadImage(multipartFile, "receipts/" + receiptFileName);
            payment.setReceiptUrl(uploadedUrl);

            // ✅ Save updates
            paymentRepository.save(payment);
            user.setTotalPaid(newPaidAmount);
            user.setPendingAmount(newPendingAmount);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment marked successfully",
                    "paymentId", payment.getId(),
                    "receiptUrl", uploadedUrl,
                    "newPaidAmount", newPaidAmount,
                    "newPendingAmount", newPendingAmount
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Failed to mark payment: " + e.getMessage()));
        }
    }

    // ✅ Monthly revenue summary
    @GetMapping("/revenue")
    @Transactional
    public ResponseEntity<?> getRevenueSummary() {
        try {
            Query query = entityManager.createNativeQuery(
                    "SELECT EXTRACT(MONTH FROM payment_date) AS month, " +
                    "EXTRACT(YEAR FROM payment_date) AS year, " +
                    "SUM(amount) AS total " +
                    "FROM payments " +
                    "GROUP BY EXTRACT(YEAR FROM payment_date), EXTRACT(MONTH FROM payment_date) " +
                    "ORDER BY year, month"
            );

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            Map<String, Object> revenueData = new HashMap<>();
            revenueData.put("january", 0.0);
            revenueData.put("february", 0.0);
            revenueData.put("march", 0.0);
            revenueData.put("april", 0.0);
            revenueData.put("may", 0.0);
            revenueData.put("june", 0.0);
            revenueData.put("july", 0.0);
            revenueData.put("august", 0.0);
            revenueData.put("september", 0.0);
            revenueData.put("october", 0.0);
            revenueData.put("november", 0.0);
            revenueData.put("december", 0.0);

            Map<Integer, String> monthNames = Map.ofEntries(
                Map.entry(1, "january"), Map.entry(2, "february"), Map.entry(3, "march"),
                Map.entry(4, "april"), Map.entry(5, "may"), Map.entry(6, "june"),
                Map.entry(7, "july"), Map.entry(8, "august"), Map.entry(9, "september"),
                Map.entry(10, "october"), Map.entry(11, "november"), Map.entry(12, "december")
            );

            double totalRevenue = 0.0;
            LocalDate today = LocalDate.now();
            double todayRevenue = 0.0;

            for (Object[] row : results) {
                int month = ((Number) row[0]).intValue();
                int year = ((Number) row[1]).intValue();
                double total = ((Number) row[2]).doubleValue();

                String monthName = monthNames.get(month);
                if (monthName != null) {
                    revenueData.put(monthName, total);
                    totalRevenue += total;
                    if (year == today.getYear() && month == today.getMonthValue()) {
                        todayRevenue += total;
                    }
                }
            }

            revenueData.put("totalRevenue", totalRevenue);
            revenueData.put("todayRevenue", todayRevenue);
            return ResponseEntity.ok(revenueData);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch revenue data: " + e.getMessage()));
        }
    }

    // ✅ Custom date range revenue
    @GetMapping("/revenue/custom")
    @Transactional
    public ResponseEntity<?> getCustomRevenue(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Double revenue = paymentService.getCustomDateRevenue(startDate, endDate);
            return ResponseEntity.ok(Map.of(
                    "startDate", startDate,
                    "endDate", endDate,
                    "revenue", revenue
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("message", "Failed to fetch custom revenue data: " + e.getMessage()));
        }
    }
}
