package es.terencio.erp.pos.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a POS device.
 * Contains the core business logic and rules for devices.
 */
public class Device {

    private final UUID id;
    private final UUID storeId;
    private final String serialCode;
    private final String hardwareId;
    private DeviceStatus status;
    private Instant lastSyncAt;

    public Device(UUID id, UUID storeId, String serialCode, String hardwareId) {
        this.id = id;
        this.storeId = storeId;
        this.serialCode = serialCode;
        this.hardwareId = hardwareId;
        this.status = DeviceStatus.ACTIVE;
        this.lastSyncAt = Instant.now();
    }

    public Device(UUID id, UUID storeId, String serialCode, String hardwareId,
            DeviceStatus status, Instant lastSyncAt) {
        this.id = id;
        this.storeId = storeId;
        this.serialCode = serialCode;
        this.hardwareId = hardwareId;
        this.status = status;
        this.lastSyncAt = lastSyncAt;
    }

    // Business methods

    public void activate() {
        this.status = DeviceStatus.ACTIVE;
    }

    public void deactivate() {
        this.status = DeviceStatus.INACTIVE;
    }

    public void updateLastSync() {
        this.lastSyncAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == DeviceStatus.ACTIVE;
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getSerialCode() {
        return serialCode;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    /**
     * Device status enum.
     */
    public enum DeviceStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
    }
}
