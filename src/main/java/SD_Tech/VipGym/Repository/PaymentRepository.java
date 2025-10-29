package SD_Tech.VipGym.Repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Entity.User;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Find payments by user
    List<Payment> findByUser(User user);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.user.id = :userId")
    Double sumAmountByUserId(@Param("userId") Long userId);
    
    List<Payment> findByUserId(Long userId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.paymentDate BETWEEN :startDate AND :endDate")
    Double findRevenueBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT SUM(p.amount) FROM Payment p")
    Double findTotalRevenue();

	List<Payment> findByUserIdOrderByPaymentDateDesc(Long id);

	 @Query("SELECT p FROM Payment p JOIN FETCH p.user ORDER BY p.paymentDate DESC")
	 List<Payment> findAllWithUser();
}
