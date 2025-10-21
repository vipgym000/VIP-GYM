package SD_Tech.VipGym.Controller;

import SD_Tech.VipGym.Entity.Admin;
import SD_Tech.VipGym.Repository.AdminRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminLoginController {

    @Autowired
    private AdminRepository adminRepository;

    // DTO for login request
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        Admin admin = adminRepository.findByUsernameAndPassword(request.getUsername(), request.getPassword());
        if (admin != null) {
            // You can generate a token or session here. For now just return success.
            return ResponseEntity.ok("Login successful");
        }
        return ResponseEntity.status(401).body("Invalid username or password");
    }
}
