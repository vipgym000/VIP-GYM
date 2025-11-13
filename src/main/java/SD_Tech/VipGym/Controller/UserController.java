package SD_Tech.VipGym.Controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import SD_Tech.VipGym.Dto.UserRegistrationRequest;
import SD_Tech.VipGym.Entity.Membership;
import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Entity.User;
import SD_Tech.VipGym.Entity.UserStatus;
import SD_Tech.VipGym.Repository.MembershipRepository;
import SD_Tech.VipGym.Repository.PaymentRepository;
import SD_Tech.VipGym.Repository.UserRepository;
import SD_Tech.VipGym.Service.ByteArrayMultipartFile;
import SD_Tech.VipGym.Service.GoogleDriveUploadService;
import SD_Tech.VipGym.Service.ReceiptImageService;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@RestController
@RequestMapping("/admin/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private GoogleDriveUploadService googleDriveUploadService;

    @Autowired
    private ReceiptImageService receiptImageService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * ✅ Register a new user with membership, payment, and Drive uploads.
     */
    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    @Transactional
    public ResponseEntity<?> registerUser(
            @RequestPart("user") UserRegistrationRequest request,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        try {
            // 1️⃣ Validate required fields
            if (request.getEmail() == null || request.getFullName() == null || request.getMembershipId() == null
                    || request.getTotalFee() == null || request.getAmount() == null || request.getPaymentDate() == null
                    || request.getJoinDate() == null) {
                return ResponseEntity.badRequest().body("Missing required fields.");
            }

            // 2️⃣ Prevent duplicate users
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("Email already registered.");
            }

            // 3️⃣ Fetch membership
            Membership membership = membershipRepository.findById(request.getMembershipId()).orElse(null);
            if (membership == null) {
                return ResponseEntity.badRequest().body("Invalid membership selected.");
            }

            // 4️⃣ Calculate next due date
            LocalDate nextDueDate = request.getJoinDate().plusMonths(membership.getDurationInMonths());

            // 5️⃣ Upload profile picture to Google Drive
            String uploadedImageUrl = null;
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    System.out.println("📤 Uploading profile picture for user: " + request.getEmail());
                    uploadedImageUrl = googleDriveUploadService.uploadToDrive(
                            profilePicture,
                            "profile-" + request.getEmail().replace("@", "_") + ".jpg"
                    );
                    System.out.println("✅ Profile picture uploaded successfully: " + uploadedImageUrl);
                } catch (Exception ex) {
                    System.err.println("⚠️ Profile picture upload failed: " + ex.getMessage());
                    ex.printStackTrace();
                    // Continue with user registration even if image upload fails
                }
            }

            // 6️⃣ Determine user status
            UserStatus status;
            try {
                status = request.getStatus() == null
                        ? UserStatus.ACTIVE
                        : UserStatus.valueOf(request.getStatus().toUpperCase());
            } catch (IllegalArgumentException ex) {
                return ResponseEntity.badRequest().body("Invalid status. Must be 'ACTIVE' or 'INACTIVE'.");
            }

            // 7️⃣ Create user
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setMobileNumber(request.getMobileNumber());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setJoinDate(request.getJoinDate());
            user.setProfilePictureUrl(uploadedImageUrl);
            user.setMembership(membership);
            user.setStatus(status);
            user.setNextDueDate(nextDueDate);
            user.setTotalPaid(0.0);
            user.setPendingAmount(request.getTotalFee());
            user = userRepository.save(user);

            // 8️⃣ Compute payment progress
            double totalPaidSoFar = paymentRepository.sumAmountByUserId(user.getId()) == null
                    ? 0.0
                    : paymentRepository.sumAmountByUserId(user.getId());
            double newPaidAmount = totalPaidSoFar + request.getAmount();
            double newPendingAmount = Math.max(request.getTotalFee() - newPaidAmount, 0.0);

            // 9️⃣ Create payment
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setPaymentDate(request.getPaymentDate());
            payment.setAmount(request.getAmount());
            payment.setPaymentMethod(request.getPaymentMethod());
            payment.setRemarks(request.getRemarks());
            payment.setPaidAmount(newPaidAmount);
            payment.setPendingAmount(newPendingAmount);
            payment.setNextDueDate(nextDueDate);

            // 🔟 Generate and upload receipt to Drive
            String receiptUrl = null;
            try {
                byte[] receiptImageBytes = receiptImageService.generateReceiptImage(
                        user.getFullName(),
                        user.getEmail(),
                        payment.getPaymentDate(),
                        payment.getAmount(),
                        membership.getName(),
                        payment.getPaymentMethod() != null ? payment.getPaymentMethod() : "Online"
                );

                String receiptFileName = "receipt-" + user.getId() + "-" + System.currentTimeMillis() + ".png";
                MultipartFile receiptFile = new ByteArrayMultipartFile(receiptImageBytes, receiptFileName, "image/png");

                System.out.println("📤 Uploading receipt for user: " + user.getEmail());
                receiptUrl = googleDriveUploadService.uploadToDrive(receiptFile, receiptFileName);
                payment.setReceiptUrl(receiptUrl);
                System.out.println("✅ Receipt uploaded successfully: " + receiptUrl);
            } catch (Exception ex) {
                System.err.println("⚠️ Receipt upload failed: " + ex.getMessage());
                ex.printStackTrace();
                // Continue with payment processing even if receipt upload fails
            }

            // 11️⃣ Save payment and update user totals
            paymentRepository.save(payment);
            user.setTotalPaid(newPaidAmount);
            user.setPendingAmount(newPendingAmount);
            user.setNextDueDate(nextDueDate);
            userRepository.save(user);

            // ✅ Response
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());
            response.put("paymentId", payment.getId());
            response.put("receiptUrl", receiptUrl);
            response.put("nextDueDate", nextDueDate);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    // ✅ Fetch all users with membership and payment details
    @GetMapping("/all")
    @Transactional
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            for (User user : users) {
                if (user.getMembership() != null) {
                    user.setMembership(membershipRepository.findById(user.getMembership().getId()).orElse(null));
                }

                List<Payment> payments = paymentRepository.findByUserIdOrderByPaymentDateDesc(user.getId());
                if (user.getPayments() == null) {
                    user.setPayments(payments);
                } else {
                    user.getPayments().clear();
                    user.getPayments().addAll(payments);
                }
            }
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to fetch users: " + e.getMessage());
        }
    }

    // ✅ Get single user with all related info
    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            EntityGraph<?> graph = entityManager.createEntityGraph(User.class);
            graph.addAttributeNodes("fullName", "email", "mobileNumber", "joinDate", "status", "totalPaid", "pendingAmount", "nextDueDate", "profilePictureUrl");
            graph.addSubgraph("membership").addAttributeNodes("id", "name", "durationInMonths", "fee");
            graph.addSubgraph("payments").addAttributeNodes("id", "paymentDate", "amount", "paymentMethod", "receiptUrl");

            User user = entityManager.createQuery("SELECT u FROM User u WHERE u.id = :id", User.class)
                    .setParameter("id", id)
                    .setHint("jakarta.persistence.fetchgraph", graph)
                    .getSingleResult();

            if (user == null) return ResponseEntity.status(404).body("User not found with ID: " + id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to fetch user: " + e.getMessage());
        }
    }

    // ✅ Delete user + profile + receipts from Google Drive
    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found with ID: " + id);
            }

            List<Payment> payments = paymentRepository.findByUserId(id);
            for (Payment payment : payments) {
                if (payment.getReceiptUrl() != null && payment.getReceiptUrl().contains("id=")) {
                    try {
                        String fileId = payment.getReceiptUrl().split("id=")[1];
                        googleDriveUploadService.deleteFile(fileId);
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to delete receipt: " + e.getMessage());
                    }
                }
            }

            if (user.getProfilePictureUrl() != null && user.getProfilePictureUrl().contains("id=")) {
                try {
                    String fileId = user.getProfilePictureUrl().split("id=")[1];
                    googleDriveUploadService.deleteFile(fileId);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to delete profile picture: " + e.getMessage());
                }
            }

            paymentRepository.deleteAll(payments);
            userRepository.deleteById(id);

            return ResponseEntity.ok("✅ User and all Drive files deleted successfully. ID: " + id);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("❌ Failed to delete user: " + e.getMessage());
        }
    }

    // ✅ Block user
    @PostMapping("/block/{id}")
    @Transactional
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) return ResponseEntity.status(404).body("User not found with ID: " + id);

            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
            return ResponseEntity.ok("🚫 User blocked successfully (ID: " + id + ")");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to block user: " + e.getMessage());
        }
    }

    // ✅ Unblock user
    @PostMapping("/unblock/{id}")
    @Transactional
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) return ResponseEntity.status(404).body("User not found with ID: " + id);

            if (user.getStatus() == UserStatus.ACTIVE)
                return ResponseEntity.badRequest().body("User is already active.");

            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            return ResponseEntity.ok("✅ User unblocked successfully (ID: " + id + ")");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to unblock user: " + e.getMessage());
        }
    }
}
