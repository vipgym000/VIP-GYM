package SD_Tech.VipGym.Controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import SD_Tech.VipGym.Service.EmailService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class WhatsAppRedirectController {

    private final EmailService emailService;

    public WhatsAppRedirectController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/api/whatsapp/send")
    public RedirectView redirectToWhatsApp(
            @RequestParam String phoneNumber,
            @RequestParam(required = false) String nextDueDate,
            HttpServletRequest request) throws UnsupportedEncodingException {

        // 🧹 1️⃣ Clean input (remove +, spaces, brackets, dashes, etc.)
        String cleanedNumber = phoneNumber.replaceAll("[^\\d]", "");

        // ☎️ 2️⃣ Ensure country code (default to India 91 if 10 digits)
        if (cleanedNumber.length() == 10) {
            cleanedNumber = "91" + cleanedNumber;
        }

        // 🧾 3️⃣ Generate message text
        String message;
        if (nextDueDate != null && !nextDueDate.isBlank()) {
            LocalDate dueDate = LocalDate.parse(nextDueDate);
            message = emailService.generateExpiryMessage(dueDate);
        } else {
            message = "Hello! 👋 This is a message from VipGym.";
        }

        // 🔐 4️⃣ Encode safely
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

        // 🌐 5️⃣ Build *universal WhatsApp link* (works on both PC + mobile)
        String whatsappUrl = "https://wa.me/" + cleanedNumber + "?text=" + encodedMessage;

        System.out.println("✅ Redirecting to WhatsApp: " + whatsappUrl);

        // 🚀 6️⃣ Redirect to WhatsApp (works across platforms)
        return new RedirectView(whatsappUrl);
    }
}
