package es.terencio.erp.devices.application.dto;

import jakarta.validation.constraints.NotBlank;

public record SetupConfirmRequest(
    @NotBlank(message = "Registration code is required") String code,
    @NotBlank(message = "Hardware ID is required") String hardwareId
) {}