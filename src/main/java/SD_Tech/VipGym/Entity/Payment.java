package SD_Tech.VipGym.Entity;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Payment → User
    @ManyToOne(fetch = FetchType.EAGER) // fetch user always
    @JoinColumn(name = "user_id")
    @JsonBackReference("user-payments")
    private User user;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(length = 500)
    private String receiptUrl;

    @Column(nullable = false)
    private Double amount;

    private String paymentMethod;
    private String remarks;

    @Column(nullable = false)
    private Double paidAmount;

    @Column(nullable = false)
    private Double pendingAmount;

    @Column(nullable = false)
    private LocalDate nextDueDate;
}
