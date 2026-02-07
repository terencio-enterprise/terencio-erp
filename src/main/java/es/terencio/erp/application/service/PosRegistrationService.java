package es.terencio.erp.application.service;

import es.terencio.erp.application.dto.PosRegistrationPreviewDto;
import es.terencio.erp.application.dto.PosRegistrationResultDto;
import es.terencio.erp.application.port.in.PosRegistrationUseCase;
import es.terencio.erp.infrastructure.adapter.out.persistence.RegistrationRepository;
import es.terencio.erp.infrastructure.adapter.out.persistence.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class PosRegistrationService implements PosRegistrationUseCase {

    private final RegistrationRepository registrationRepo;
    private final UserRepository userRepo;

    public PosRegistrationService(RegistrationRepository registrationRepo, UserRepository userRepo) {
        this.registrationRepo = registrationRepo;
        this.userRepo = userRepo;
    }

    @Override
    public PosRegistrationPreviewDto previewRegistration(String code) {
        var info = registrationRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid registration code"));

        if (info.isUsed()) {
            throw new IllegalStateException("Code already used");
        }
        if (info.expiresAt().isBefore(Instant.now())) {
            throw new IllegalStateException("Code expired");
        }

        // Fetch users for this store to sync initially
        var users = userRepo.findUsersByStore(info.storeId());

        // Generate a logical POS ID (e.g., POS-MAD-001-XXXX)
        // In a real app, this might come from the code table or be sequential
        String generatedPosId = "POS-" + info.code(); 

        return new PosRegistrationPreviewDto(
                generatedPosId,
                info.preassignedName(),
                info.storeId().toString(),
                info.storeName(),
                "PENDING_HARDWARE_ID", // Placeholder
                users
        );
    }

    @Override
    @Transactional
    public PosRegistrationResultDto confirmRegistration(String code, String hardwareId) {
        var info = registrationRepo.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid code"));

        if (info.isUsed()) {
            throw new IllegalStateException("Code already used");
        }

        // Generate Logical Serial (e.g. POS-01)
        String serialCode = "POS-" + code; 

        UUID deviceId = registrationRepo.registerDevice(
                code, 
                hardwareId, 
                info.storeId(),
                serialCode
        );

        return new PosRegistrationResultDto(
                info.storeId(),
                info.storeName(),
                deviceId,
                serialCode,
                UUID.randomUUID().toString() // Generate a license key
        );
    }
}