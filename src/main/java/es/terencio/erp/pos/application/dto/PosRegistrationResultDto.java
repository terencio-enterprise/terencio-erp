package es.terencio.erp.pos.application.dto;

import java.util.UUID;

/**
 * Response DTO for POS registration confirmation.
 * Contains device and license information.
 */
public record PosRegistrationResultDto(
        UUID storeId,
        String storeName,
        UUID deviceId,
        String serialCode,
        String licenseKey) {
}
