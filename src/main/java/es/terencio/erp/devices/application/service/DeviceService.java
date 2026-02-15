package es.terencio.erp.devices.application.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.auth.infrastructure.security.DeviceApiKeyGenerator;
import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.dto.GenerateCodeRequest;
import es.terencio.erp.devices.application.dto.GeneratedCodeDto;
import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupResultDto;
import es.terencio.erp.devices.application.port.in.ManageDevicesUseCase;
import es.terencio.erp.devices.application.port.in.SetupDeviceUseCase;
import es.terencio.erp.devices.application.port.out.DevicePort;
import es.terencio.erp.shared.domain.SerialGenerator;
import es.terencio.erp.shared.exception.RegistrationException;
import es.terencio.erp.shared.exception.ResourceNotFoundException;
import es.terencio.erp.users.application.port.out.UserPort;

@Service
public class DeviceService implements ManageDevicesUseCase, SetupDeviceUseCase {

    private final DevicePort devicePort;
    private final UserPort userPort;
    private final DeviceApiKeyGenerator apiKeyGenerator;
    private final SecureRandom random = new SecureRandom();

    public DeviceService(DevicePort devicePort, UserPort userPort,
            DeviceApiKeyGenerator apiKeyGenerator) {
        this.devicePort = devicePort;
        this.userPort = userPort;
        this.apiKeyGenerator = apiKeyGenerator;
    }

    @Override
    public List<DeviceDto> listAll() {
        return devicePort.findAll();
    }

    @Override
    @Transactional
    public void blockDevice(UUID id) {
        devicePort.updateStatus(id, "BLOCKED");
    }

    @Override
    @Transactional
    public void unblockDevice(UUID id) {
        devicePort.updateStatus(id, "ACTIVE");
    }

    @Override
    @Transactional
    public GeneratedCodeDto generateRegistrationCode(GenerateCodeRequest request) {
        String code = generateUniqueCode();
        int hours = request.validityHours() != null ? request.validityHours() : 24;
        Instant expiresAt = Instant.now().plus(hours, ChronoUnit.HOURS);

        devicePort.saveCode(code, request.storeId(), request.posName(), expiresAt);
        return new GeneratedCodeDto(code, request.posName(), expiresAt);
    }

    private String generateUniqueCode() {
        for (int i = 0; i < 5; i++) {
            int num = 100000 + random.nextInt(900000);
            String code = String.valueOf(num);
            if (devicePort.findByCode(code).isEmpty())
                return code;
        }
        throw new RuntimeException("Failed to generate code");
    }

    // --- Public Setup ---

    @Override
    public SetupPreviewDto previewSetup(String code) {
        var info = devicePort.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Registration code not found"));
        validateCode(info);

        // Load users associated with this store
        var storeUsers = userPort.findByStoreId(info.storeId());

        return new SetupPreviewDto(
                "POS-" + info.storeCode() + "-" + info.code(),
                info.preassignedName(),
                info.storeId().toString(),
                info.storeName(),
                storeUsers);
    }

    @Override
    @Transactional
    public SetupResultDto confirmSetup(String code, String hardwareId) {
        var info = devicePort.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Registration code not found"));
        validateCode(info);

        // Generate device-specific secret
        String deviceSecret = apiKeyGenerator.generateDeviceSecret();

        // Register device with plaintext secret (database encryption handles security)
        String serialCode = SerialGenerator.generate(info.storeCode());
        UUID deviceId = devicePort.registerDevice(code, hardwareId, info.storeId(), serialCode, deviceSecret);

        // Generate API key (device will use this for authentication)
        String apiKey = apiKeyGenerator.generateApiKey(deviceId, deviceSecret, 1);

        return new SetupResultDto(
                info.storeId(),
                info.storeName(),
                deviceId,
                serialCode,
                apiKey);
    }

    private void validateCode(DevicePort.CodeInfo info) {
        if (info.isUsed())
            throw new RegistrationException("Code already used");
        if (info.expiresAt().isBefore(Instant.now()))
            throw new RegistrationException("Code expired");
    }
}