package SD_Tech.VipGym.Repository;

import SD_Tech.VipGym.Entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {

    boolean existsByName(String name);

}
