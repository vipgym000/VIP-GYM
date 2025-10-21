package SD_Tech.VipGym.Controller;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Repository.PaymentRepository;
import SD_Tech.VipGym.Service.EmailService;

@RestController
@RequestMapping("/api/reminder")
@CrossOrigin(origins = "*")
public class ReminderController {

    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    public ReminderController(PaymentRepository paymentRepository, EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
    }

    // ✅ Get message (no email) for frontend display
    @GetMapping("/message/{userId}")
    public Map<String, Object> getReminderMessage(@PathVariable Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);

        if (payments.isEmpty()) {
            return Map.of("status", "No payment records found");
        }

        Payment latestPayment = payments.stream()
                .max(Comparator.comparing(Payment::getNextDueDate))
                .orElse(null);

        if (latestPayment == null)
            return Map.of("status", "No valid due date found");

        String message = emailService.generateExpiryMessage(latestPayment.getNextDueDate());

        return Map.of(
                "userId", userId,
                "nextDueDate", latestPayment.getNextDueDate(),
                "message", message
        );
    }

    // ✅ Manually trigger email from frontend
    @PostMapping("/send/{userId}")
    public Map<String, Object> sendReminderEmail(@PathVariable Long userId) {
        List<Payment> payments = paymentRepository.findByUserId(userId);
        if (payments.isEmpty()) {
            return Map.of("status", "No payment records found for user");
        }

        Payment latestPayment = payments.stream()
                .max(Comparator.comparing(Payment::getNextDueDate))
                .orElse(null);

        if (latestPayment == null)
            return Map.of("status", "No valid due date found");

        String message = emailService.generateExpiryMessage(latestPayment.getNextDueDate());
        emailService.sendEmail(latestPayment.getUser().getEmail(), "Membership Expiry Reminder", message);

        return Map.of(
                "status", "Email sent successfully",
                "to", latestPayment.getUser().getEmail(),
                "message", message
        );
    }
}
