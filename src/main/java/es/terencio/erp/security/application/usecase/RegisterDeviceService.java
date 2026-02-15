package es.terencio.erp.security.application.usecase;

import java.util.UUID;

import es.terencio.erp.security.application.port.in.RegisterDeviceUseCase;
import es.terencio.erp.security.application.port.out.DeviceRepository;
import es.terencio.erp.security.application.port.out.RegistrationCodeRepository;
import es.terencio.erp.security.domain.model.Device;
import es.terencio.erp.security.domain.model.RegistrationCode;
import es.terencio.erp.shared.exception.DomainException;

/**
 * Use case for registering devices using registration codes.
 */
public class RegisterDeviceService implements RegisterDeviceUseCase {

    private final DeviceRepository deviceRepository;
    private final RegistrationCodeRepository registrationCodeRepository;

    public RegisterDeviceService(
            DeviceRepository deviceRepository,
            RegistrationCodeRepository registrationCodeRepository) {
        this.deviceRepository = deviceRepository;
        this.registrationCodeRepository = registrationCodeRepository;
    }

    @Override
    public RegisterDeviceResult execute(RegisterDeviceCommand command) {
        // Validate registration code
        RegistrationCode regCode = registrationCodeRepository.findByCode(command.registrationCode())
                .orElseThrow(() -> new DomainException("Invalid registration code"));

        if (!regCode.isValid()) {
            throw new DomainException("Registration code is expired or already used");
        }

        // Check if hardware already registered
        if (deviceRepository.findByHardwareId(command.hardwareId()).isPresent()) {
            throw new DomainException("Device hardware already registered");
        }

        // Determine serial code (use preassigned or generate)
        String serialCode = regCode.preassignedName() != null
                ? regCode.preassignedName()
                : generateSerialCode();

        // Validate unique serial within store
        if (deviceRepository.existsByStoreAndSerialCode(regCode.storeId(), serialCode)) {
            throw new DomainException("Serial code already exists in this store");
        }

        // Generate device secret
        String deviceSecret = UUID.randomUUID().toString();

        // Register device
        Device device = Device.register(
                regCode.storeId(),
                serialCode,
                serialCode,
                command.hardwareId(),
                deviceSecret);

        device.updateVersion(command.appVersion());
        device.activate();

        Device saved = deviceRepository.save(device);

        // Mark registration code as used
        regCode.consume(saved.id());
        registrationCodeRepository.save(regCode);

        return new RegisterDeviceResult(
                saved.id().value(),
                saved.storeId().value(),
                saved.serialCode(),
                saved.deviceSecret());
    }

    private String generateSerialCode() {
        return "DEV-" + System.currentTimeMillis();
    }
}
