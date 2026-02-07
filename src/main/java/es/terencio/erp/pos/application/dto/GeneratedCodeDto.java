package es.terencio.erp.pos.application.dto;

import java.time.Instant;

/**
 * Response returned to the Admin.
 * They give this 'code' to the person setting up the physical POS.
 */
public record GeneratedCodeDto(
        String code,
        String posName,
        Instant expiresAt) {
}