package SD_Tech.VipGym.Controller;

import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Service.PaymentService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ✅ Get all payments of a particular user
    @GetMapping("/user/{userId}")
    public Map<String, Object> getUserPayments(@PathVariable Long userId) {
        List<Payment> payments = paymentService.getPaymentsByUser(userId);
        Integer daysLeft = paymentService.getDaysUntilPlanExpires(payments);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("payments", payments);
        response.put("daysLeftForPlan", daysLeft != null ? daysLeft : "No active plan");

        return response;
    }

    // ✅ Get complete, week, month, and year revenue
    @GetMapping("/revenue")
    public Map<String, Object> getRevenueSummary() {
        return paymentService.getRevenueSummary();
    }

    // ✅ Get custom date range revenue
    @GetMapping("/revenue/custom")
    public Map<String, Object> getCustomRevenue(
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        Double revenue = paymentService.getCustomDateRevenue(start, end);

        return Map.of(
                "startDate", start,
                "endDate", end,
                "customRevenue", revenue
        );
    }
}
