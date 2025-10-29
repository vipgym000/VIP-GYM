package SD_Tech.VipGym.Controller;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

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
            LocalDate dueDate = parseFlexibleDate(nextDueDate);
            message = emailService.generateExpiryMessage(dueDate);
        } else {
            message = "Hello! 👋 This is a message from VipGym.";
        }

        // 🔐 4️⃣ Encode safely
        String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8);

        // 🌐 5️⃣ Build universal WhatsApp link (works on both mobile + PC)
        String whatsappUrl = "https://wa.me/" + cleanedNumber + "?text=" + encodedMessage;

        // 🪄 6️⃣ Logging for debug
        System.out.println("✅ Redirecting to WhatsApp: " + whatsappUrl);
        System.out.println("📱 Final WhatsApp number: " + cleanedNumber);
        System.out.println("🗓️ Original date input: " + nextDueDate);

        // 🚀 7️⃣ Redirect to WhatsApp
        return new RedirectView(whatsappUrl);
    }

    /**
     * Safely parses multiple date formats (handles both ISO and JS date strings).
     */
    private LocalDate parseFlexibleDate(String input) {
        try {
            // 🟢 ISO format (2025-11-29)
            return LocalDate.parse(input);
        } catch (DateTimeParseException e1) {
            try {
                // 🟠 JS/Frontend format (Sat Nov 29 2025)
                DateTimeFormatter jsFormat = DateTimeFormatter.ofPattern("EEE MMM dd yyyy", Locale.ENGLISH);
                return LocalDate.parse(input, jsFormat);
            } catch (DateTimeParseException e2) {
                try {
                    // 🔵 Alternative long format (Saturday, November 29, 2025)
                    DateTimeFormatter altFormat = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy", Locale.ENGLISH);
                    return LocalDate.parse(input, altFormat);
                } catch (DateTimeParseException e3) {
                    System.err.println("⚠️ Could not parse date: " + input + " → Defaulting to today");
                    return LocalDate.now();
                }
            }
        }
    }
}
