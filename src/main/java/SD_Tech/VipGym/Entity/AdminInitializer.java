package SD_Tech.VipGym.Entity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import SD_Tech.VipGym.Repository.AdminRepository;

@Configuration
public class AdminInitializer {

    @Bean
    CommandLineRunner initAdmin(AdminRepository adminRepository) {
        return args -> {
            if (!adminRepository.existsById("Admin")) {
                Admin admin = new Admin("Admin", "Admin123");
                adminRepository.save(admin);
                System.out.println("Admin user created.");
            }
        };
    }
}
