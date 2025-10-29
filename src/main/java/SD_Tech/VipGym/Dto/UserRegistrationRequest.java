package SD_Tech.VipGym.Dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String status;
}
