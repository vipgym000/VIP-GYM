package SD_Tech.VipGym.Controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import SD_Tech.VipGym.Service.EmailService;

@RestController
public class WhatsAppRedirectController {

    private final EmailService emailService;

    public WhatsAppRedirectController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/api/whatsapp/send")
    public RedirectView redirectToWhatsApp(
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String nextDueDate) throws UnsupportedEncodingException {

        // 🧹 Clean the phone number — digits only, ensure no leading +
        String cleanedNumber = phoneNumber.replaceAll("[^\\d]", "");

        // ✅ Ensure country code (e.g., India => 91)
        if (!cleanedNumber.startsWith("91") && cleanedNumber.length() == 10) {
            cleanedNumber = "91" + cleanedNumber;
        }

        // 🧾 Generate the message
        String message;
        if (nextDueDate != null && !nextDueDate.isBlank()) {
            LocalDate dueDate = LocalDate.parse(nextDueDate);
            message = emailService.generateExpiryMessage(dueDate);
        } else {
            message = "Hello! 👋 This is a message from VipGym.";
        }

        // ✅ Encode only once
        String encodedMessage = URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);

        // ✅ Use WhatsApp API URL — works on all mobile devices
        String whatsappUrl = "https://api.whatsapp.com/send?phone=" + cleanedNumber + "&text=" + encodedMessage;

        System.out.println("Redirecting to WhatsApp URL: " + whatsappUrl);

        return new RedirectView(whatsappUrl);
    }
}
