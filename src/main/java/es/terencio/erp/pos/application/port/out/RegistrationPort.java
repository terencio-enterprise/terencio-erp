package es.terencio.erp.pos.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port for registration-related operations.
 * Application layer depends on this interface.
 * Infrastructure layer provides the implementation.
 */
public interface RegistrationPort {

    /**
     * Find registration information by code.
     * 
     * @param code the registration code
     * @return registration info if found
     */
    Optional<RegistrationInfo> findByCode(String code);

    /**
     * Register a new device and mark the registration code as used.
     * 
     * @param code       registration code
     * @param hardwareId device hardware ID
     * @param storeId    store UUID
     * @param serialCode generated serial code
     * @return the newly created device UUID
     */
    UUID registerDevice(String code, String hardwareId, UUID storeId, String serialCode);

    /**
     * Data structure for registration information.
     */
    record RegistrationInfo(
            String code,
            String preassignedName,
            Instant expiresAt,
            Boolean isUsed,
            UUID storeId,
            String storeName,
            String storeCode) {
    }
}
