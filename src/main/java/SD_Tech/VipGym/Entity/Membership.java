package SD_Tech.VipGym.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memberships")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;    // e.g., "Monthly", "Quarterly", "Annual"

    @Column(nullable = false)
    private Integer durationInMonths;  // Membership duration

    @Column(nullable = false)
    private Double fee;     // Price for this membership plan
}
