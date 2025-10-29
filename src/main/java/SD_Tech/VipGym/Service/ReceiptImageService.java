package SD_Tech.VipGym.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class ReceiptImageService {

    /**
     * Generate a professional PNG receipt image in memory as byte[].
     */
    public byte[] generateReceiptImage(String userName, String email, LocalDate paymentDate, 
                                     Double amount, String membershipName, String paymentMethod) throws IOException {
        int width = 500;
        int height = 700;

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        // Enable anti-aliasing for better text rendering
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Header with background color
        g2d.setColor(new Color(102, 126, 234)); // Primary color
        g2d.fillRect(0, 0, width, 120);

        // Company name
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 28));
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth("VipGym");
        g2d.drawString("VipGym", (width - textWidth) / 2, 50);

        // Tagline
        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth("Your Fitness Journey Starts Here");
        g2d.drawString("Your Fitness Journey Starts Here", (width - textWidth) / 2, 75);

        // Receipt title
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth("PAYMENT RECEIPT");
        g2d.drawString("PAYMENT RECEIPT", (width - textWidth) / 2, 105);

        // Receipt body with border
        g2d.setColor(Color.WHITE);
        g2d.fillRect(40, 140, width - 80, height - 180);
        g2d.setColor(new Color(220, 220, 220));
        g2d.drawRect(40, 140, width - 80, height - 180);

        // Receipt details
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Receipt Details", 60, 170);

        // Receipt number and date
        String receiptNo = "RCP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(100, 100, 100));
        g2d.drawString("Receipt No: " + receiptNo, 60, 195);
        g2d.drawString("Date: " + LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), 60, 215);

        // Horizontal line
        g2d.setColor(new Color(220, 220, 220));
        g2d.drawLine(60, 230, width - 60, 230);

        // Payment details section
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Payment Information", 60, 260);

        // Customer details
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString("Customer Name:", 60, 290);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(userName, 180, 290);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString("Email:", 60, 315);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(email, 180, 315);

        // Membership details
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString("Membership Plan:", 60, 340);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(membershipName, 180, 340);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString("Payment Date:", 60, 365);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(paymentDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")), 180, 365);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString("Payment Method:", 60, 390);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(paymentMethod != null ? paymentMethod : "Online", 180, 390);

        // Horizontal line
        g2d.setColor(new Color(220, 220, 220));
        g2d.drawLine(60, 415, width - 60, 415);

        // Amount section
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("Payment Summary", 60, 445);

        // Table headers
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString("Description", 60, 475);
        g2d.drawString("Amount", 340, 475);

        // Horizontal line
        g2d.setColor(new Color(220, 220, 220));
        g2d.drawLine(60, 485, width - 60, 485);

        // Membership fee
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.setColor(new Color(60, 60, 60));
        g2d.drawString("Membership Fee", 60, 510);
        g2d.setFont(new Font("Arial", Font.BOLD, 12));
        g2d.drawString(String.format("$%.2f", amount), 340, 510);

        // Horizontal line
        g2d.setColor(new Color(220, 220, 220));
        g2d.drawLine(60, 525, width - 60, 525);

        // Total
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.BLACK);
        g2d.drawString("Total Paid:", 60, 555);
        g2d.drawString(String.format("$%.2f", amount), 340, 555);

        // Footer section
        g2d.setColor(new Color(102, 126, 234)); // Primary color
        g2d.fillRect(40, height - 120, width - 80, 120);

        // Thank you message
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth("Thank you for your payment!");
        g2d.drawString("Thank you for your payment!", (width - textWidth) / 2, height - 80);

        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth("This receipt serves as proof of payment");
        g2d.drawString("This receipt serves as proof of payment", (width - textWidth) / 2, height - 55);

        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth("For questions, contact us at support@vipgym.com");
        g2d.drawString("For questions, contact us at support@vipgym.com", (width - textWidth) / 2, height - 30);

        g2d.dispose();

        // Write to byte array output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }
}