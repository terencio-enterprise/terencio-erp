package es.terencio.erp.users.application.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateUserRequest(
    @NotBlank String fullName,
    @NotBlank String role,
    @NotNull UUID storeId,
    boolean isActive,
    List<String> permissions
) {}