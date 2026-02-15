package es.terencio.erp.security.application.usecase;

import java.util.UUID;

/**
 * Result of device registration.
 */
public record RegisterDeviceResult(
        UUID deviceId,
        UUID storeId,
        String serialCode,
        String deviceSecret) {
}
