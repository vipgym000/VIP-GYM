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

        String path = "uploads/profile_pics/" + fileName;
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", repoOwner, repoName, path);

        String base64Content = Base64.getEncoder().encodeToString(file.getBytes());

        // Prepare request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", "Upload profile picture for " + fileName);
        requestBody.put("content", base64Content);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(githubToken);

        // Check if file exists to get the SHA for updating
        try {
            HttpEntity<Void> getRequest = new HttpEntity<>(headers);
            ResponseEntity<Map> getResponse = restTemplate.exchange(url, HttpMethod.GET, getRequest, Map.class);
            if (getResponse.getStatusCode().is2xxSuccessful() && getResponse.getBody() != null) {
                String sha = (String) getResponse.getBody().get("sha");
                requestBody.put("sha", sha); // Required for updating existing file
            }
        } catch (HttpClientErrorException.NotFound e) {
            // File does not exist, no SHA needed — proceed to create new file
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to check existing file on GitHub: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> content = (Map<String, Object>) response.getBody().get("content");
                return (String) content.get("download_url"); // May not be public for private repos
            } else {
                throw new RuntimeException("GitHub upload failed with status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("GitHub upload failed: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        }
    }
    
    public void deleteAllReceipts() {
        String path = "receipts";
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s", repoOwner, repoName, path);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        HttpEntity<Void> getRequest = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, getRequest, List.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Failed to list receipts folder contents");
        }

        List<Map<String, Object>> files = response.getBody();

        for (Map<String, Object> file : files) {
            String filePath = (String) file.get("path");
            String sha = (String) file.get("sha");

            // Delete the file
            deleteFile(filePath, sha);
        }
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

    public void deleteSingleReceiptFile(String filePath) {
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
        requestBody.put("message", "Delete receipt " + filePath);
        requestBody.put("sha", sha);

        HttpHeaders deleteHeaders = new HttpHeaders();
        deleteHeaders.setContentType(MediaType.APPLICATION_JSON);
        deleteHeaders.setBearerAuth(githubToken);

        HttpEntity<Map<String, Object>> deleteRequest = new HttpEntity<>(requestBody, deleteHeaders);

        ResponseEntity<Map> deleteResponse = restTemplate.exchange(url, HttpMethod.DELETE, deleteRequest, Map.class);

        if (!deleteResponse.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to delete file: " + filePath);
        }
    }
}
