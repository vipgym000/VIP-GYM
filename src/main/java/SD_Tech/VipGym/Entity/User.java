package SD_Tech.VipGym.Entity;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // ✅ prevents ByteBuddy proxy serialization errors
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String mobileNumber;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private LocalDate joinDate;

    @Column(nullable = false)
    private Double totalPaid = 0.0;

    @Column(nullable = false)
    private Double pendingAmount = 0.0;

    @Column(nullable = false)
    private LocalDate nextDueDate;

    private String profilePictureUrl;

    // ✅ Active membership (current one in effect)
    @ManyToOne(optional = false)
    @JoinColumn(name = "membership_id")
    @JsonManagedReference("membership-users")
    private Membership membership;

    // ✅ Scheduled (future) membership — set only when switching
    @ManyToOne
    @JoinColumn(name = "next_membership_id")
    private Membership nextMembership;

    // ✅ Whether a membership switch is scheduled
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean membershipSwitchPending = false;

    // ✅ All payments linked to this user
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference("user-payments")
    private List<Payment> payments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'ACTIVE'")
    private UserStatus status;
}
