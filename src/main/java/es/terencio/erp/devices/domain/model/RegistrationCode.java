package es.terencio.erp.devices.domain.model;

import java.time.Instant;

import es.terencio.erp.shared.domain.exception.InvalidStateException;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.StoreId;

/**
 * RegistrationCode entity.
 * One-time code used for device onboarding.
 */
public class RegistrationCode {

    private final String code;
    private final StoreId storeId;
    private String preassignedName;
    private final Instant expiresAt;
    private boolean used;
    private Instant usedAt;
    private DeviceId usedByDeviceId;
    private final Instant createdAt;

    public RegistrationCode(
            String code,
            StoreId storeId,
            String preassignedName,
            Instant expiresAt,
            boolean used,
            Instant usedAt,
            DeviceId usedByDeviceId,
            Instant createdAt) {

        if (code == null || code.isBlank())
            throw new InvariantViolationException("Registration code cannot be empty");
        if (storeId == null)
            throw new InvariantViolationException("Registration code must be tied to a store");
        if (expiresAt == null)
            throw new InvariantViolationException("Expiration date cannot be null");

        this.code = code;
        this.storeId = storeId;
        this.preassignedName = preassignedName;
        this.expiresAt = expiresAt;
        this.used = used;
        this.usedAt = usedAt;
        this.usedByDeviceId = usedByDeviceId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static RegistrationCode generate(StoreId storeId, String preassignedName, Instant expiresAt) {
        String code = generateCode();
        return new RegistrationCode(
                code,
                storeId,
                preassignedName,
                expiresAt,
                false,
                null,
                null,
                Instant.now());
    }

    public void consume(DeviceId deviceId) {
        if (used) {
            throw new InvalidStateException("Registration code already used");
        }
        if (Instant.now().isAfter(expiresAt)) {
            throw new InvalidStateException("Registration code expired");
        }

        this.used = true;
        this.usedAt = Instant.now();
        this.usedByDeviceId = deviceId;
    }

    public boolean isValid() {
        return !used && Instant.now().isBefore(expiresAt);
    }

    private static String generateCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    // Getters
    public String code() {
        return code;
    }

    public StoreId storeId() {
        return storeId;
    }

    public String preassignedName() {
        return preassignedName;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public Instant usedAt() {
        return usedAt;
    }

    public DeviceId usedByDeviceId() {
        return usedByDeviceId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
