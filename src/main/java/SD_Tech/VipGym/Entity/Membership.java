package SD_Tech.VipGym.Entity;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "memberships")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // ✅ prevents proxy serialization error
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
    
    @OneToMany(mappedBy = "membership")
    @JsonIgnore // ✅ prevents infinite recursion when serializing User
    private List<User> users;
}
