package es.terencio.erp.security.domain.model;

import es.terencio.erp.shared.domain.exception.InvalidStateException;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.StoreId;

import java.time.Instant;

/**
 * Device aggregate root.
 * Represents a POS terminal/device.
 */
public class Device {

    private final DeviceId id;
    private final StoreId storeId;
    private String name;
    private final String serialCode; // Logical ID (e.g., "CAJA-01")
    private final String hardwareId; // Physical fingerprint
    private DeviceStatus status;
    private String versionApp;
    private String deviceSecret;
    private int apiKeyVersion;
    private Instant lastAuthenticatedAt;
    private Instant lastSyncAt;
    private final Instant createdAt;
    private Instant updatedAt;

    public Device(
            DeviceId id,
            StoreId storeId,
            String name,
            String serialCode,
            String hardwareId,
            DeviceStatus status,
            String versionApp,
            String deviceSecret,
            int apiKeyVersion,
            Instant lastAuthenticatedAt,
            Instant lastSyncAt,
            Instant createdAt,
            Instant updatedAt) {

        if (id == null)
            throw new InvariantViolationException("Device ID cannot be null");
        if (storeId == null)
            throw new InvariantViolationException("Device must belong to a store");
        if (serialCode == null || serialCode.isBlank())
            throw new InvariantViolationException("Serial code cannot be empty");
        if (hardwareId == null || hardwareId.isBlank())
            throw new InvariantViolationException("Hardware ID cannot be empty");

        this.id = id;
        this.storeId = storeId;
        this.name = name;
        this.serialCode = serialCode;
        this.hardwareId = hardwareId;
        this.status = status != null ? status : DeviceStatus.PENDING;
        this.versionApp = versionApp;
        this.deviceSecret = deviceSecret;
        this.apiKeyVersion = apiKeyVersion;
        this.lastAuthenticatedAt = lastAuthenticatedAt;
        this.lastSyncAt = lastSyncAt;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static Device register(
            StoreId storeId,
            String name,
            String serialCode,
            String hardwareId,
            String deviceSecret) {

        return new Device(
                DeviceId.create(),
                storeId,
                name,
                serialCode,
                hardwareId,
                DeviceStatus.PENDING,
                null,
                deviceSecret,
                1,
                null,
                null,
                Instant.now(),
                Instant.now());
    }

    public void activate() {
        if (status == DeviceStatus.ACTIVE) {
            throw new InvalidStateException("Device is already active");
        }
        this.status = DeviceStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void block() {
        this.status = DeviceStatus.BLOCKED;
        this.updatedAt = Instant.now();
    }

    public void updateVersion(String version) {
        this.versionApp = version;
        this.updatedAt = Instant.now();
    }

    public void recordAuthentication() {
        if (status != DeviceStatus.ACTIVE) {
            throw new InvalidStateException("Cannot authenticate non-active device");
        }
        this.lastAuthenticatedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void recordSync() {
        this.lastSyncAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == DeviceStatus.ACTIVE;
    }

    // Getters
    public DeviceId id() {
        return id;
    }

    public StoreId storeId() {
        return storeId;
    }

    public String name() {
        return name;
    }

    public String serialCode() {
        return serialCode;
    }

    public String hardwareId() {
        return hardwareId;
    }

    public DeviceStatus status() {
        return status;
    }

    public String versionApp() {
        return versionApp;
    }

    public String deviceSecret() {
        return deviceSecret;
    }

    public int apiKeyVersion() {
        return apiKeyVersion;
    }

    public Instant lastAuthenticatedAt() {
        return lastAuthenticatedAt;
    }

    public Instant lastSyncAt() {
        return lastSyncAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
