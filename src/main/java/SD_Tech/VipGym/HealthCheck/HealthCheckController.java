package SD_Tech.VipGym.HealthCheck;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        System.out.println("Server is live");
        return ResponseEntity.ok("OK");
    }
}
