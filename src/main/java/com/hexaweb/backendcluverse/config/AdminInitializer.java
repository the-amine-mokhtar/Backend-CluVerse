package com.hexaweb.backendcluverse.config;

import com.hexaweb.backendcluverse.entities.User;
import com.hexaweb.backendcluverse.repositories.UserRepository;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    public AdminInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@cluverse.tn";
        if (userRepository.findFirstByEmailOrderByIdDesc(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setFirstName("cluverse");
            admin.setLastName("admin");
            admin.setEmail(adminEmail);
            admin.setPassword(BCrypt.hashpw("123", BCrypt.gensalt()));
            admin.setPhone("40862777");
            admin.setSuperAdmin(true);
            userRepository.save(admin);
            System.out.println("Super admin user created successfully.");
        }
    }
}
