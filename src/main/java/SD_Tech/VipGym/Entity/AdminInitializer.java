package SD_Tech.VipGym.Entity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import SD_Tech.VipGym.Repository.AdminRepository;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!adminRepository.existsById("Admin")) {
                String encodedPassword = passwordEncoder.encode("Admin123");
                Admin admin = new Admin("Admin", encodedPassword);
                adminRepository.save(admin);
                System.out.println("Admin user created with encoded password.");
            }
        };
    }
}
