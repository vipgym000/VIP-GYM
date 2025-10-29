package SD_Tech.VipGym.Dto;

import java.time.LocalDate;

public record PaymentHistoryDTO(
    Long id,
    LocalDate paymentDate,
    Double amount,
    String paymentMethod,
    String receiptUrl,
    String userFullName,
    String userEmail,
    String userProfilePictureUrl
) {}
