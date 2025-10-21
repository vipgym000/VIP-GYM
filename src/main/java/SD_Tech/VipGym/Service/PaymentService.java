package SD_Tech.VipGym.Service;

import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public List<Payment> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    public Map<String, Object> getRevenueSummary() {
        Map<String, Object> summary = new HashMap<>();

        LocalDate now = LocalDate.now();
        LocalDate weekStart = now.minusDays(7);
        LocalDate monthStart = now.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate yearStart = now.with(TemporalAdjusters.firstDayOfYear());

        summary.put("totalRevenue", Optional.ofNullable(paymentRepository.findTotalRevenue()).orElse(0.0));
        summary.put("weekRevenue", Optional.ofNullable(paymentRepository.findRevenueBetween(weekStart, now)).orElse(0.0));
        summary.put("monthRevenue", Optional.ofNullable(paymentRepository.findRevenueBetween(monthStart, now)).orElse(0.0));
        summary.put("yearRevenue", Optional.ofNullable(paymentRepository.findRevenueBetween(yearStart, now)).orElse(0.0));

        return summary;
    }

    public Double getCustomDateRevenue(LocalDate startDate, LocalDate endDate) {
        return Optional.ofNullable(paymentRepository.findRevenueBetween(startDate, endDate)).orElse(0.0);
    }

    public Integer getDaysUntilPlanExpires(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getNextDueDate)
                .max(LocalDate::compareTo)
                .map(due -> LocalDate.now().until(due).getDays())
                .orElse(null);
    }
}
