package es.terencio.erp.pos.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for POS registration confirmation.
 */
public record PosRegistrationConfirmRequest(
        @NotBlank(message = "Registration code is required") String code,

        @NotBlank(message = "Hardware ID is required") String hardwareId) {
}
