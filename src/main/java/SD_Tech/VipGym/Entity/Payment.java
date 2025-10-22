package SD_Tech.VipGym.Entity;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    @JsonBackReference
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
    private Double paidAmount;      // Total paid by user till this payment (inclusive)

    @Column(nullable = false)
    private Double pendingAmount;   // Remaining amount after this payment

    @Column(nullable = false)
    private LocalDate nextDueDate;  // Updated due date after payment
}
