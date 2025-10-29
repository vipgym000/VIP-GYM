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

        // 🧹 1️⃣ Clean the number (remove +, spaces, dashes, etc.)
        String cleanedNumber = phoneNumber.replaceAll("[^\\d]", "");

        // 🪄 2️⃣ Auto-prepend country code (India: 91)
        if (cleanedNumber.length() == 10) {
            cleanedNumber = "91" + cleanedNumber;
        }

        // 🧾 3️⃣ Create message
        String message;
        if (nextDueDate != null && !nextDueDate.isBlank()) {
            LocalDate dueDate = LocalDate.parse(nextDueDate);
            message = emailService.generateExpiryMessage(dueDate);
        } else {
            message = "Hello! 👋 This is a message from VipGym.";
        }

        // 🔐 4️⃣ Encode safely
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

        // 🌐 5️⃣ Detect device type (mobile or desktop)
        String userAgent = request.getHeader("User-Agent");
        boolean isMobile = userAgent != null && userAgent.toLowerCase().matches(".*(android|iphone|ipad|mobile).*");

        // 🪄 6️⃣ Choose proper base URL
        String baseUrl = isMobile
                ? "https://api.whatsapp.com/send"
                : "https://web.whatsapp.com/send";

        // ✅ 7️⃣ Build full redirect URL
        String whatsappUrl = baseUrl + "?phone=" + cleanedNumber + "&text=" + encodedMessage;

        System.out.println("Redirecting to WhatsApp: " + whatsappUrl);

        return new RedirectView(whatsappUrl);
    }
}
