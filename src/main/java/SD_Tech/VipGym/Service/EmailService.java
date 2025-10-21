package SD_Tech.VipGym.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    private final String fromEmail = "vipgym000@gmail.com";

    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("✅ Email sent successfully to " + to);
        } catch (Exception e) {
            System.err.println("❌ Failed to send email: " + e.getMessage());
        }
    }

    // 🧠 Generate membership expiry message
    public String generateExpiryMessage(LocalDate nextDueDate) {
        LocalDate today = LocalDate.now();

        long daysDiff = ChronoUnit.DAYS.between(today, nextDueDate);

        if (daysDiff > 1)
            return "Your membership will expire in " + daysDiff + " days (on " + nextDueDate + "). Please renew soon!";
        else if (daysDiff == 1)
            return "Your membership will expire tomorrow (" + nextDueDate + "). Please renew today!";
        else if (daysDiff == 0)
            return "Your membership expires today! Renew now to continue your access.";
        else
            return "Your membership expired " + Math.abs(daysDiff) + " days ago (" + nextDueDate + "). Please renew immediately!";
    }
}
