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
import SD_Tech.VipGym.Service.GitHubImageUploadService;
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
    private GitHubImageUploadService gitHubImageUploadService;

    @Autowired
    private ReceiptImageService receiptImageService;
    
    @PersistenceContext
    private EntityManager entityManager;

    // ... (registerUser, getAllUsers, getUserById, blockUser, unblockUser methods remain the same) ...

    /**
     * Register a new user with membership and payment details.
     * Accepts profile picture as a file (uploaded to GitHub).
     * Generates and uploads payment receipt image to GitHub, returns its URL.
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

            // 4️⃣ Calculate next due date *before saving user*
            LocalDate nextDueDate = request.getJoinDate().plusMonths(membership.getDurationInMonths());

            // 5️⃣ Upload profile picture (optional)
            String uploadedImageUrl = null;
            if (profilePicture != null && !profilePicture.isEmpty()) {
                try {
                    uploadedImageUrl = gitHubImageUploadService.uploadImage(
                            profilePicture,
                            "user-" + request.getEmail().replace("@", "_") + ".jpg"
                    );
                } catch (Exception ex) {
                    System.err.println("⚠️ Profile picture upload failed: " + ex.getMessage());
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

            // 7️⃣ Create and save new user (with all non-null fields)
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setMobileNumber(request.getMobileNumber());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setJoinDate(request.getJoinDate());
            user.setProfilePictureUrl(uploadedImageUrl);
            user.setMembership(membership);
            user.setStatus(status);

            // ✅ Set these BEFORE saving (to satisfy DB constraints)
            user.setNextDueDate(nextDueDate);
            user.setTotalPaid(0.0);
            user.setPendingAmount(request.getTotalFee());

            user = userRepository.save(user); // ✅ will not throw now

            // 8️⃣ Compute payment progress
            double totalPaidSoFar = paymentRepository.sumAmountByUserId(user.getId()) == null
                    ? 0.0
                    : paymentRepository.sumAmountByUserId(user.getId());
            double newPaidAmount = totalPaidSoFar + request.getAmount();
            double newPendingAmount = Math.max(request.getTotalFee() - newPaidAmount, 0.0);

            // 9️⃣ Create Payment entity
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setPaymentDate(request.getPaymentDate());
            payment.setAmount(request.getAmount());
            payment.setPaymentMethod(request.getPaymentMethod());
            payment.setRemarks(request.getRemarks());
            payment.setPaidAmount(newPaidAmount);
            payment.setPendingAmount(newPendingAmount);
            payment.setNextDueDate(nextDueDate);

            // 🔟 Generate and upload receipt safely
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
                receiptUrl = gitHubImageUploadService.uploadImage(receiptFile, "receipts/" + receiptFileName);
                payment.setReceiptUrl(receiptUrl);
            } catch (Exception ex) {
                System.err.println("⚠️ Receipt upload failed: " + ex.getMessage());
            }

            // 11️⃣ Save payment
            paymentRepository.save(payment);

            // 12️⃣ Update user progress summary
            user.setTotalPaid(newPaidAmount);
            user.setPendingAmount(newPendingAmount);
            user.setNextDueDate(nextDueDate);
            userRepository.save(user);

            // 13️⃣ Response
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

    // Fetch all users with their payments and membership details
    @GetMapping("/all")
    @Transactional
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();

            for (User user : users) {
                // Ensure membership is fetched
                if (user.getMembership() != null) {
                    user.setMembership(
                        membershipRepository.findById(user.getMembership().getId()).orElse(null)
                    );
                }

                // Fetch and attach payments safely
                List<Payment> payments = paymentRepository.findByUserIdOrderByPaymentDateDesc(user.getId());
                if (user.getPayments() != null) {
                    user.getPayments().clear();
                    user.getPayments().addAll(payments);
                } else {
                    user.setPayments(payments);
                }
            }

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to fetch users: " + e.getMessage());
        }
    }
    
    // Get user with all details by ID
    @GetMapping("/{id}")
    @Transactional
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            // Create an entity graph to fetch user with their payments and membership
            EntityGraph<?> graph = entityManager.createEntityGraph(User.class);
            graph.addAttributeNodes("fullName", "email", "mobileNumber", "joinDate", "status", "totalPaid", "pendingAmount", "nextDueDate", "profilePictureUrl");
            graph.addSubgraph("membership").addAttributeNodes("id", "name", "durationInMonths", "fee");
            graph.addSubgraph("payments").addAttributeNodes("id", "paymentDate", "amount", "paymentMethod", "receiptUrl", "paidAmount", "pendingAmount", "nextDueDate");
            
            User user = entityManager.createQuery("SELECT u FROM User u WHERE u.id = :id", User.class)
                    .setParameter("id", id)
                    .setHint("jakarta.persistence.fetchgraph", graph)
                    .getSingleResult();
            
            if (user == null) {
                return ResponseEntity.status(404).body("User not found with ID: " + id);
            }
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Failed to fetch user: " + e.getMessage());
        }
    }
    
    // ✅ FINAL CORRECTED VERSION: Delete user and all related data
 // ✅ FINAL FIXED VERSION — deletes user + all receipts under /uploads/profile_pics/receipts/
    @DeleteMapping("/delete/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            // 1️⃣ Fetch user
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found with ID: " + id);
            }

            // 2️⃣ Fetch all payments first
            List<Payment> payments = paymentRepository.findByUserId(id);
            System.out.println("🧾 Found " + payments.size() + " payments to delete receipts for.");

            // 3️⃣ Delete each payment’s receipt from GitHub
            for (Payment payment : payments) {
                if (payment.getReceiptUrl() != null && !payment.getReceiptUrl().isBlank()) {
                    try {
                        // Extract the filename safely
                        String fileName = payment.getReceiptUrl()
                                .substring(payment.getReceiptUrl().lastIndexOf("/") + 1);

                        // ✅ Match your actual folder structure
                        String filePath = "uploads/profile_pics/receipts/" + fileName;

                        gitHubImageUploadService.deleteSingleReceiptFile(filePath);
                        System.out.println("✅ Deleted receipt: " + filePath);
                    } catch (Exception e) {
                        System.err.println("⚠️ Failed to delete receipt for payment ID " + payment.getId() + ": " + e.getMessage());
                    }
                }
            }

            // 4️⃣ Delete user’s profile picture
            if (user.getProfilePictureUrl() != null && !user.getProfilePictureUrl().isBlank()) {
                try {
                    String fileName = user.getProfilePictureUrl()
                            .substring(user.getProfilePictureUrl().lastIndexOf("/") + 1);

                    String profilePath = "uploads/profile_pics/" + fileName;

                    gitHubImageUploadService.deleteSingleReceiptFile(profilePath);
                    System.out.println("✅ Deleted profile picture: " + profilePath);
                } catch (Exception e) {
                    System.err.println("⚠️ Failed to delete profile picture for user ID " + id + ": " + e.getMessage());
                }
            }

            // 5️⃣ Delete all payment records
            paymentRepository.deleteAll(payments);

            // 6️⃣ Finally, delete user
            userRepository.deleteById(id);
            System.out.println("🗑️ Deleted user record from DB for ID: " + id);

            // 7️⃣ Success response
            return ResponseEntity.ok("✅ User and all related data (payments, receipts, profile picture) deleted successfully. ID: " + id);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body("❌ Failed to delete user and related data: " + e.getMessage());
        }
    }

    // 🔹 Block user (mark as INACTIVE)
    @PostMapping("/block/{id}")
    @Transactional
    public ResponseEntity<?> blockUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found with ID: " + id);
            }

            user.setStatus(UserStatus.INACTIVE);
            userRepository.save(user);
            return ResponseEntity.ok("User has been blocked successfully (ID: " + id + ")");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to block user: " + e.getMessage());
        }
    }
    
    // 🔹 Unblock user (mark as ACTIVE again)
    @PostMapping("/unblock/{id}")
    @Transactional
    public ResponseEntity<?> unblockUser(@PathVariable Long id) {
        try {
            User user = userRepository.findById(id).orElse(null);
            if (user == null) {
                return ResponseEntity.status(404).body("User not found with ID: " + id);
            }

            if (user.getStatus() == UserStatus.ACTIVE) {
                return ResponseEntity.badRequest().body("User is already active.");
            }

            user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);

            return ResponseEntity.ok("✅ User has been unblocked successfully (ID: " + id + ")");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Failed to unblock user: " + e.getMessage());
        }
    }
}