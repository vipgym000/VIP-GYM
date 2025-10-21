package SD_Tech.VipGym.Repository;

import SD_Tech.VipGym.Entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Optional: add custom queries if needed, e.g. findByEmail
    boolean existsByEmail(String email);
}
