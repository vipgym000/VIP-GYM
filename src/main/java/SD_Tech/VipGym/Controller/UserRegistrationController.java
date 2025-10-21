package SD_Tech.VipGym.Controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import SD_Tech.VipGym.Dto.UserRegistrationRequest;
import SD_Tech.VipGym.Entity.Membership;
import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Entity.User;
import SD_Tech.VipGym.Repository.MembershipRepository;
import SD_Tech.VipGym.Repository.PaymentRepository;
import SD_Tech.VipGym.Repository.UserRepository;
import SD_Tech.VipGym.Service.GitHubImageUploadService;
import SD_Tech.VipGym.Service.ReceiptImageService;
import SD_Tech.VipGym.Service.ByteArrayMultipartFile;

@RestController
@RequestMapping("/admin/users")
public class UserRegistrationController {

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

    /**
     * Register a new user with membership and payment details.
     * Accepts profile picture as a file (uploaded to GitHub).
     * Generates and uploads payment receipt image to GitHub, returns its URL.
     */
    @PostMapping(value = "/register", consumes = {"multipart/form-data"})
    public ResponseEntity<?> registerUser(
            @RequestPart("user") UserRegistrationRequest request,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {

        try {
            // Validate basic required fields
            if (request.getEmail() == null || request.getFullName() == null || request.getMembershipId() == null
                    || request.getTotalFee() == null || request.getAmount() == null || request.getPaymentDate() == null) {
                return ResponseEntity.badRequest().body("Missing required fields.");
            }

            // Check if email already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body("Email already registered");
            }

            // Fetch membership by ID
            Membership membership = membershipRepository.findById(request.getMembershipId())
                    .orElse(null);
            if (membership == null) {
                return ResponseEntity.badRequest().body("Invalid membership selected");
            }

            // Upload profile picture (if provided)
            String uploadedImageUrl = null;
            if (profilePicture != null && !profilePicture.isEmpty()) {
                uploadedImageUrl = gitHubImageUploadService.uploadImage(
                        profilePicture,
                        "user-" + request.getEmail().replace("@", "_") + ".jpg"
                );
            }

            // Create user entity
            User user = new User();
            user.setFullName(request.getFullName());
            user.setEmail(request.getEmail());
            user.setMobileNumber(request.getMobileNumber());
            user.setDateOfBirth(request.getDateOfBirth());
            user.setJoinDate(request.getJoinDate());
            user.setProfilePictureUrl(uploadedImageUrl);
            user.setMembership(membership);

            // Save user to get ID
            user = userRepository.save(user);

            // Calculate total paid so far (should be zero since new user)
            Double totalPaidSoFar = paymentRepository.sumAmountByUserId(user.getId());
            if (totalPaidSoFar == null) totalPaidSoFar = 0.0;

            Double newPaidAmount = totalPaidSoFar + request.getAmount();
            Double newPendingAmount = request.getTotalFee() - newPaidAmount;
            if (newPendingAmount < 0) newPendingAmount = 0.0;

            // Calculate next due date = joinDate + membership duration
            LocalDate nextDueDate = request.getJoinDate().plusMonths(membership.getDurationInMonths());

            // Create payment entity
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setPaymentDate(request.getPaymentDate());
            payment.setAmount(request.getAmount());
            payment.setPaymentMethod(request.getPaymentMethod());
            payment.setRemarks(request.getRemarks());
            payment.setPaidAmount(newPaidAmount);
            payment.setPendingAmount(newPendingAmount);
            payment.setNextDueDate(nextDueDate);

            // Generate receipt image bytes
            byte[] receiptImageBytes = receiptImageService.generateReceiptImage(
                    user.getFullName(),
                    user.getEmail(),
                    payment.getPaymentDate(),
                    payment.getAmount(),
                    membership.getName()
            );

            String receiptFileName = "receipt-" + user.getId() + "-" + System.currentTimeMillis() + ".png";

            // Convert bytes to MultipartFile
            MultipartFile receiptMultipartFile = new ByteArrayMultipartFile(receiptImageBytes, receiptFileName, "image/png");

            // Upload receipt image to GitHub
            String receiptUrl = gitHubImageUploadService.uploadImage(receiptMultipartFile, "receipts/" + receiptFileName);

            // Set receipt URL in payment entity
            payment.setReceiptUrl(receiptUrl);

            // Save payment
            paymentRepository.save(payment);

            // Prepare response data
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("receiptUrl", receiptUrl);
            response.put("userId", user.getId());
            response.put("paymentId", payment.getId());

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Image upload failed: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/receipts")
    public ResponseEntity<String> deleteAllReceipts() {
        try {
            gitHubImageUploadService.deleteAllReceipts();
            return ResponseEntity.ok("All receipts deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete receipts: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/receipt/{fileName}")
    public ResponseEntity<String> deleteSingleReceipt(@PathVariable String fileName) {
        try {
            gitHubImageUploadService.deleteSingleReceiptFile("receipts/" + fileName);
            return ResponseEntity.ok("Receipt " + fileName + " deleted successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to delete receipt: " + e.getMessage());
        }
    }

}
