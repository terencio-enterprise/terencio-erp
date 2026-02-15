package es.terencio.erp.security.domain.model;

/**
 * Device status lifecycle.
 */
public enum DeviceStatus {
    PENDING, // Waiting for registration
    ACTIVE, // Operating normally
    BLOCKED // Suspended/blocked
}
