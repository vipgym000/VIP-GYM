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
        
        // Remove any non-digit characters from phone number
        String cleanedNumber = phoneNumber.replaceAll("[^\\d]", "");
        
        String message;
        if (nextDueDate != null) {
            LocalDate dueDate = LocalDate.parse(nextDueDate);
            message = emailService.generateExpiryMessage(dueDate);
        } else {
            message = "Hello! This is a message from VipGym.";
        }
        
        // URL encode the message
        String encodedMessage = URLEncoder.encode(message, "UTF-8");
        
        // Construct WhatsApp URL
        String whatsappUrl = "https://wa.me/" + cleanedNumber + "?text=" + encodedMessage;
        
        return new RedirectView(whatsappUrl);
    }
}
