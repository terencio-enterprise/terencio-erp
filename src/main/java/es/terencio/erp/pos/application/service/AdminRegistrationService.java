package es.terencio.erp.pos.application.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.pos.application.dto.GenerateCodeRequest;
import es.terencio.erp.pos.application.dto.GeneratedCodeDto;
import es.terencio.erp.pos.application.port.in.ManageRegistrationCodeUseCase;
import es.terencio.erp.pos.application.port.out.RegistrationPort;

@Service
public class AdminRegistrationService implements ManageRegistrationCodeUseCase {

    private final RegistrationPort registrationPort;
    private final SecureRandom random = new SecureRandom();

    public AdminRegistrationService(RegistrationPort registrationPort) {
        this.registrationPort = registrationPort;
    }

    @Override
    @Transactional
    public GeneratedCodeDto generateCode(GenerateCodeRequest request) {
        // 1. Generate a unique 6-digit code
        String code = generateUniqueCode();

        // 2. Calculate Expiration (Default 24 hours)
        int hours = request.validityHours() != null ? request.validityHours() : 24;
        Instant expiresAt = Instant.now().plus(hours, ChronoUnit.HOURS);

        // 3. Save to DB via Port
        registrationPort.saveCode(code, request.storeId(), request.posName(), expiresAt);

        // 4. Return to Admin
        return new GeneratedCodeDto(code, request.posName(), expiresAt);
    }

    private String generateUniqueCode() {
        // Simple retry logic to ensure uniqueness
        for (int i = 0; i < 5; i++) {
            int num = 100000 + random.nextInt(900000); // 100000 to 999999
            String code = String.valueOf(num);

            // Check if exists via port (we reuse the existing findByCode method)
            if (registrationPort.findByCode(code).isEmpty()) {
                return code;
            }
        }
        throw new RuntimeException("Failed to generate a unique code after retries");
    }
}