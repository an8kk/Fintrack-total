package com.fintrack.backend.config;

import com.fintrack.backend.entity.User;
import com.fintrack.backend.repository.UserRepository;
import com.fintrack.backend.service.AuthService;
import com.fintrack.backend.service.CategorizationService;
import com.fintrack.backend.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final CategorizationService categorizationService;

    @Override
    public void run(String... args) throws Exception {
        log.info("DataInitializer started");

        // Seed merchant category map for hybrid categorization
        try {
            categorizationService.seedDefaults();
            log.info("Merchant category map seeded successfully");
        } catch (Exception e) {
            log.error("Failed to seed merchant category map", e);
        }

        userRepository.findByUsername("Anuar").ifPresentOrElse(user -> {
            log.info("User 'Anuar' found. Current Salt Edge ID: {}", user.getSaltEdgeCustomerId());
            if (user.getSaltEdgeCustomerId() == null) {
                user.setSaltEdgeCustomerId("1742284256719149910");
                userRepository.save(user);
                log.info("Set Salt Edge Customer ID for user Anuar");
            } else {
                log.info("User 'Anuar' already has Salt Edge ID.");
            }
        }, () -> {
            log.info("User 'Anuar' NOT found. Creating...");
            User newUser = new User();
            newUser.setUsername("Anuar");
            newUser.setEmail("anuar@example.com");
            newUser.setPassword("password");
            newUser.setRole(Role.USER);
            newUser.setSaltEdgeCustomerId("1742284256719149910");

            try {
                authService.register(newUser);
                log.info("Created user 'Anuar' and triggered Salt Edge import.");
            } catch (Exception e) {
                log.error("Failed to create user 'Anuar'", e);
            }
        });
    }
}
