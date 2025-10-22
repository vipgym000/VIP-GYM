package SD_Tech.VipGym.Dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class UserRegistrationRequest {
    private String fullName;
    private String email;
    private String mobileNumber;
    private LocalDate dateOfBirth;
    private LocalDate joinDate;
    private String profilePictureUrl;

    private Long membershipId;

    private LocalDate paymentDate;
    private Double amount;
    private String paymentMethod;
    private String remarks;
    private Double totalFee;

    // 👇 Add this line
    private String status; // should be either "ACTIVE" or "INACTIVE"
}
