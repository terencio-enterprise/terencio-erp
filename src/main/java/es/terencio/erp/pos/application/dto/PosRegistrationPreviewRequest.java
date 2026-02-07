package es.terencio.erp.pos.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POS registration preview.
 */
public record PosRegistrationPreviewRequest(
        @NotBlank(message = "Registration code is required") String code,

        @NotBlank(message = "Device ID is required") String deviceId) {
}
