package com.collectx.iam.config;

import com.collectx.iam.entity.Role;
import com.collectx.iam.entity.User;
import com.collectx.iam.entity.UserStatus;
import com.collectx.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        seedAdmin(
            "Admin",
            "admin@collectx.com",
            "Admin@1234"
        );
    }

    private void seedAdmin(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            log.info("Admin already exists — skipping seed for email={}", email);
            return;
        }

        User admin = new User();
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin.setStatus(UserStatus.ACTIVE);
        admin.setFailedAttempts(0);
        userRepository.save(admin);

        log.info("Default admin seeded — email={}", email);
    }
}
