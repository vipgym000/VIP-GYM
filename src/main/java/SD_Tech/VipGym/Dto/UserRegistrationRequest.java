package SD_Tech.VipGym.Dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserRegistrationRequest {
    // User fields
    private String fullName;
    private String email;
    private String mobileNumber;
    private LocalDate dateOfBirth;
    private LocalDate joinDate;
    private String profilePictureUrl;

    // Membership selection
    private Long membershipId;

    // Payment fields
    private LocalDate paymentDate;
    private Double amount;
    private String paymentMethod;
    private String remarks;

    // Total membership fee for the user (needed to calculate pending amount)
    private Double totalFee;
}
