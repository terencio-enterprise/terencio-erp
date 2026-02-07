package es.terencio.erp.pos.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request from the Admin Panel to generate a new setup code for a POS.
 */
public record GenerateCodeRequest(
        @NotNull(message = "Store ID is required") UUID storeId,

        @NotBlank(message = "Pre-assigned POS name is required") String posName,

        // Optional: How long the code is valid (hours). Default 24 if null.
        Integer validityHours) {
}