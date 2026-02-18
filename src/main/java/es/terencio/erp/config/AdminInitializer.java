package es.terencio.erp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.terencio.erp.employees.application.port.out.EmployeePort;

@Configuration
public class AdminInitializer {

    private static final Logger logger = LoggerFactory.getLogger(AdminInitializer.class);

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.pin}")
    private String adminPin;

    @Bean
    CommandLineRunner initAdmin(EmployeePort employeePort, PasswordEncoder passwordEncoder) {
        return args -> {
            if (employeePort.findByUsername(adminUsername).isEmpty()) {
                logger.info("Creating default admin employee...");
                String encodedPassword = passwordEncoder.encode(adminPassword);
                String encodedPin = passwordEncoder.encode(adminPin);

                employeePort.save(
                        adminUsername,
                        "Administrator",
                        "ADMIN",
                        encodedPin,
                        encodedPassword,
                        null, // companyId
                        null, // storeId
                        "[]" // permissions
                );
                logger.info("Default admin employee created: {}", adminUsername);
            } else {
                logger.debug("Admin employee already exists.");
            }
        };
    }
}
