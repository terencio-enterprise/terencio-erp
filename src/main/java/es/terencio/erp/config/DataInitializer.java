package es.terencio.erp.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import es.terencio.erp.users.application.port.out.UserPort;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserPort userPort;
    private final PasswordEncoder passwordEncoder;

    // Default store ID from V2__registration_system.sql migration
    private static final UUID DEFAULT_STORE_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");

    public DataInitializer(UserPort userPort, PasswordEncoder passwordEncoder) {
        this.userPort = userPort;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
    }

    private void initializeAdminUser() {
        if (userPort.findByUsername("admin").isEmpty()) {
            log.info("Creating default admin user...");

            String pinHash = passwordEncoder.encode("123456"); // 6-digit POS PIN
            String passwordHash = passwordEncoder.encode("admin"); // Backoffice password

            Long userId = userPort.save(
                    "admin",
                    "Administrator",
                    "ADMIN",
                    pinHash,
                    passwordHash,
                    DEFAULT_STORE_ID,
                    "[]" // Empty permissions JSON
            );

            log.info("✅ Default admin user created with ID: {} (username: admin, password: admin)", userId);
        } else {
            log.info("Admin user already exists, skipping initialization");
        }
    }
}
