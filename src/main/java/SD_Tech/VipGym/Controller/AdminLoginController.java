package SD_Tech.VipGym.Controller;

import SD_Tech.VipGym.Entity.Admin;
import SD_Tech.VipGym.Repository.AdminRepository;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminLoginController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // DTO for login request
    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername());

        if (admin == null) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), admin.getPassword());

        if (passwordMatches) {
            // TODO: Generate token or session here for real authentication
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }
}
