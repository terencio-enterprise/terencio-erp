package es.terencio.erp.config;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.DependsOn;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import es.terencio.erp.users.application.port.out.UserPort;

@Component
@DependsOn("flywayInitializer")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserPort userPort;
    private final PasswordEncoder passwordEncoder;
    private final FlywayMigrationInitializer flywayInitializer;

    // Default IDs from seed data in migration file (lines 697-704)
    private static final UUID DEFAULT_COMPANY_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID DEFAULT_STORE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    public DataInitializer(UserPort userPort, PasswordEncoder passwordEncoder,
            FlywayMigrationInitializer flywayInitializer) {
        this.userPort = userPort;
        this.passwordEncoder = passwordEncoder;
        this.flywayInitializer = flywayInitializer;
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
                    DEFAULT_COMPANY_ID,
                    DEFAULT_STORE_ID,
                    "[]" // Empty permissions JSON
            );

            log.info("✅ Default admin user created with ID: {} (username: admin, password: admin)", userId);
        } else {
            log.info("Admin user already exists, skipping initialization");
        }
    }
}
