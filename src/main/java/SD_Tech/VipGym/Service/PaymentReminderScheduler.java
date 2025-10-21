package SD_Tech.VipGym.Service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import SD_Tech.VipGym.Entity.Payment;
import SD_Tech.VipGym.Repository.PaymentRepository;

@Component
public class PaymentReminderScheduler {

    private final PaymentRepository paymentRepository;
    private final EmailService emailService;

    public PaymentReminderScheduler(PaymentRepository paymentRepository, EmailService emailService) {
        this.paymentRepository = paymentRepository;
        this.emailService = emailService;
    }

    // 🕒 Runs every day at 8 AM
    @Scheduled(cron = "0 0 8 * * *")
    public void sendPaymentReminders() {
        List<Payment> allPayments = paymentRepository.findAll();
        LocalDate today = LocalDate.now();

        for (Payment payment : allPayments) {
            LocalDate dueDate = payment.getNextDueDate();
            if (dueDate == null) continue;

            long daysUntilDue = today.until(dueDate).getDays();

            // Send reminder 3 days before due
            if (daysUntilDue == 3) {
                sendReminderEmail(payment, "Membership Expiry Reminder (3 days left)");
            }

            // Send reminder on the day of expiry
            if (daysUntilDue == 0) {
                sendReminderEmail(payment, "Membership Expiring Today!");
            }
        }
    }

    private void sendReminderEmail(Payment payment, String subject) {
        String to = payment.getUser().getEmail();
        String message = emailService.generateExpiryMessage(payment.getNextDueDate());
        emailService.sendEmail(to, subject, message);
    }
}
