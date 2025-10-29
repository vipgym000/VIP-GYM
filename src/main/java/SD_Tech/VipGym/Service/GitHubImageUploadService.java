package SD_Tech.VipGym.Service;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

@Service
public class GitHubImageUploadService {

    @Value("${github.token}")
    private String githubToken;

    @Value("${github.repoOwner}")
    private String repoOwner;

    @Value("${github.repoName}")
    private String repoName;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Uploads (or updates) a profile picture to GitHub under /uploads/profile_pics/
     *
     * @param file     Multipart image file
     * @param fileName Desired file name (e.g., "user-rahul.jpg")
     * @return         GitHub file URL (download URL)
     * @throws IOException If file read/upload fails
     */
    public String uploadImage(MultipartFile file, String fileName) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        byte[] fileBytes = file.getBytes();

        // ✅ Compress if file is larger than 1MB
        if (fileBytes.length > 1_000_000) {
            System.out.println("⚠️ File size " + (fileBytes.length / 1024) + "KB exceeds 1MB, compressing...");
            fileBytes = compressImage(fileBytes, 0.6f); // 60% quality
            System.out.println("✅ Compressed file size: " + (fileBytes.length / 1024) + "KB");
        }

        String path = "uploads/profile_pics/" + fileName;
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", repoOwner, repoName, path);

        String base64Content = Base64.getEncoder().encodeToString(fileBytes);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", "Upload profile picture for " + fileName);
        requestBody.put("content", base64Content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(githubToken);

        // Check if file exists (get SHA if updating)
        try {
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<Map> getResponse = restTemplate.exchange(url, HttpMethod.GET, getRequest, Map.class);
            if (getResponse.getStatusCode().is2xxSuccessful() && getResponse.getBody() != null) {
                String sha = (String) getResponse.getBody().get("sha");
                requestBody.put("sha", sha);
            }
        } catch (HttpClientErrorException.NotFound e) {
            // File doesn’t exist — proceed with new upload
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> content = (Map<String, Object>) response.getBody().get("content");
                return (String) content.get("download_url");
            } else {
                throw new RuntimeException("GitHub upload failed with status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("GitHub upload failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }

    public void deleteAllReceipts() {
        String path = "uploads/profile_pics/receipts";
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", repoOwner, repoName, path);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, getRequest, List.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("❌ Failed to list contents of folder: " + path);
        }

        List<Map<String, Object>> files = response.getBody();

        int deletedCount = 0;
        for (Map<String, Object> file : files) {
            String filePath = (String) file.get("path");
            String sha = (String) file.get("sha");

            try {
                deleteFile(filePath, sha);
                deletedCount++;
            } catch (Exception ex) {
                System.err.println("⚠️ Failed to delete file: " + filePath + " (" + ex.getMessage() + ")");
            }
        }

        System.out.println("✅ Successfully deleted " + deletedCount + " receipt(s) from " + path);
    }

    private void deleteFile(String filePath, String sha) {
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", repoOwner, repoName, filePath);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", "Delete receipt " + filePath);
        requestBody.put("sha", sha);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(githubToken);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.DELETE, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to delete file: " + filePath);
        }
    }

    /**
     * Deletes a single file from the GitHub repository.
     * This method is robust and will not fail if the file does not exist.
     *
     * @param filePath The full path to the file in the repo (e.g., "receipts/receipt-1-123.png")
     */
    public void deleteSingleReceiptFile(String filePath) {
        try {
            String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", repoOwner, repoName, filePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(githubToken);
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);

            // First get file info to obtain SHA
            ResponseEntity<Map> getResponse = restTemplate.exchange(url, HttpMethod.GET, getRequest, Map.class);

            if (!getResponse.getStatusCode().is2xxSuccessful() || getResponse.getBody() == null) {
                throw new RuntimeException("File not found: " + filePath);
            }

            String sha = (String) getResponse.getBody().get("sha");

            // Prepare delete request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", "Delete file " + filePath);
            requestBody.put("sha", sha);

            HttpHeaders deleteHeaders = new HttpHeaders();
            deleteHeaders.setContentType(MediaType.APPLICATION_JSON);
            deleteHeaders.setBearerAuth(githubToken);

            HttpEntity<Map<String, Object>> deleteRequest = new HttpEntity<>(requestBody, deleteHeaders);

            restTemplate.exchange(url, HttpMethod.DELETE, deleteRequest, Map.class);

        } catch (HttpClientErrorException.NotFound e) {
            // ✅ Gracefully handle case where file doesn't exist. This is key for robust cleanup.
            System.err.println("INFO: File not found on GitHub, skipping deletion: " + filePath);
        } catch (Exception e) {
            // Re-throw other exceptions as they might be real issues (e.g., auth, network)
            throw new RuntimeException("Failed to delete file: " + filePath, e);
        }
    }
    
    private byte[] compressImage(byte[] imageBytes, float quality) throws IOException {
        // 1️⃣ Reject too-large raw uploads (saves heap)
        if (imageBytes.length > 10_000_000) {
            throw new IOException("Image too large (>" + (imageBytes.length / 1024 / 1024) + " MB). Please upload below 10 MB.");
        }

        // 2️⃣ Decode safely
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bais);
        if (image == null) {
            throw new IOException("Invalid image file, cannot decode.");
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // 3️⃣ Downscale extremely large images to keep memory low
        int maxDimension = 720;
        if (width > maxDimension || height > maxDimension) {
            float scale = Math.min((float) maxDimension / width, (float) maxDimension / height);
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);

            BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            resized.getGraphics().drawImage(image, 0, 0, newWidth, newHeight, java.awt.Color.WHITE, null);
            image.flush(); // free memory
            image = resized;
        }

        // 4️⃣ Compress JPEG
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = jpgWriter.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality); // 0.3–0.5 works well
        }

        jpgWriter.setOutput(new MemoryCacheImageOutputStream(baos));
        jpgWriter.write(null, new IIOImage(image, null, null), param);
        jpgWriter.dispose();
        image.flush();

        byte[] compressed = baos.toByteArray();

        // 5️⃣ Recursive fallback to ensure under 2 MB
        if (compressed.length > 2_000_000 && quality > 0.2f) {
            System.out.println("⚠️ Still " + (compressed.length / 1024) + " KB, compressing more...");
            return compressImage(compressed, quality - 0.1f);
        }

        System.out.println("✅ Final compressed size: " + (compressed.length / 1024) + " KB");
        return compressed;
    }
}