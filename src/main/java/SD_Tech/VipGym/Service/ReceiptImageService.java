package SD_Tech.VipGym.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;

@Service
public class ReceiptImageService {

    /**
     * Generate a PNG receipt image in memory as byte[].
     */
    public byte[] generateReceiptImage(String userName, String email, LocalDate paymentDate, Double amount, String membershipName) throws IOException {
        int width = 400;
        int height = 250;

        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        // Draw text in black
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.drawString("VipGym Payment Receipt", 80, 30);

        g2d.setFont(new Font("Arial", Font.PLAIN, 14));
        g2d.drawString("Name: " + userName, 20, 70);
        g2d.drawString("Email: " + email, 20, 100);
        g2d.drawString("Membership: " + membershipName, 20, 130);
        g2d.drawString("Payment Date: " + paymentDate.toString(), 20, 160);
        g2d.drawString(String.format("Amount Paid: $%.2f", amount), 20, 190);

        g2d.setFont(new Font("Arial", Font.ITALIC, 12));
        g2d.drawString("Thank you for your payment!", 20, 230);

        g2d.dispose();

        // Write to byte array output stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);
        return baos.toByteArray();
    }
}
