package es.terencio.erp.employees.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateEmployeeRequest(
    @NotBlank String fullName,
    @NotBlank String role,
    @NotNull UUID storeId,
    boolean isActive,
    List<String> permissions
) {}
