package SD_Tech.VipGym.HealthCheck;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class HealthCheckScheduler {

    private final RestTemplate restTemplate = new RestTemplate();

    // Change localhost and port if needed
    private final String healthCheckUrl = "https://vip-gym.onrender.com/health";

    @Scheduled(fixedRate = 14 * 60 * 1000)  // every 14 minutes
    public void callHealthCheck() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(healthCheckUrl, String.class);
            System.out.println("Health check response: " + response.getBody());
        } catch (Exception e) {
            System.err.println("Health check failed: " + e.getMessage());
        }
    }
}
