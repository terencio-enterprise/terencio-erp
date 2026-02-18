package es.terencio.erp.devices.application.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GenerateCodeRequest(
    @NotNull(message = "Store ID is required") UUID storeId,
    @NotBlank(message = "Pre-assigned POS name is required") String posName,
    Integer validityHours
) {}
