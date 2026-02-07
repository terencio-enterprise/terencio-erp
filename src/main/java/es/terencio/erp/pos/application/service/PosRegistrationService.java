package es.terencio.erp.pos.application.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.pos.application.dto.PosRegistrationPreviewDto;
import es.terencio.erp.pos.application.dto.PosRegistrationResultDto;
import es.terencio.erp.pos.application.port.in.PosRegistrationUseCase;
import es.terencio.erp.pos.application.port.out.LoadUsersPort;
import es.terencio.erp.pos.application.port.out.RegistrationPort;
import es.terencio.erp.shared.domain.SerialGenerator;
import es.terencio.erp.shared.exception.RegistrationException;

/**
 * Application service implementing POS registration use cases.
 * Depends only on ports (interfaces), not on infrastructure implementations.
 */
@Service
public class PosRegistrationService implements PosRegistrationUseCase {

    private final RegistrationPort registrationPort;
    private final LoadUsersPort loadUsersPort;

    public PosRegistrationService(
            RegistrationPort registrationPort,
            LoadUsersPort loadUsersPort) {
        this.registrationPort = registrationPort;
        this.loadUsersPort = loadUsersPort;
    }

    @Override
    public PosRegistrationPreviewDto previewRegistration(String code) {
        var info = registrationPort.findByCode(code)
                .orElseThrow(() -> new RegistrationException("Invalid registration code"));

        if (info.isUsed()) {
            throw new RegistrationException("Registration code has already been used");
        }
        if (info.expiresAt().isBefore(Instant.now())) {
            throw new RegistrationException("Registration code has expired");
        }

        // Fetch users for this store to sync initially
        var users = loadUsersPort.loadByStore(info.storeId());

        // Generate a logical POS ID using the preassigned name
        String generatedPosId = "POS-" + info.storeCode() + "-" + info.code();

        return new PosRegistrationPreviewDto(
                generatedPosId,
                info.preassignedName(),
                info.storeId().toString(),
                info.storeName(),
                users);
    }

    @Override
    @Transactional
    public PosRegistrationResultDto confirmRegistration(String code, String hardwareId) {
        var info = registrationPort.findByCode(code)
                .orElseThrow(() -> new RegistrationException("Invalid registration code"));

        if (info.isUsed()) {
            throw new RegistrationException("Registration code has already been used");
        }

        // Generate serial code using domain service
        String serialCode = SerialGenerator.generate(info.storeCode());

        UUID deviceId = registrationPort.registerDevice(
                code,
                hardwareId,
                info.storeId(),
                serialCode);

        return new PosRegistrationResultDto(
                info.storeId(),
                info.storeName(),
                deviceId,
                serialCode,
                UUID.randomUUID().toString() // Generate a license key
        );
    }
}