package es.terencio.erp.devices.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SetupPreviewRequest(
    @NotBlank(message = "Registration code is required") String code,
    @NotBlank(message = "Device ID is required") String deviceId
) {}