package es.terencio.erp.employees.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateEmployeeRequest(
        @NotBlank String username,
        String email,
        @NotBlank @Size(min = 6, max = 6) @Pattern(regexp = "\\d+") String posPin,
        @NotBlank @Size(min = 8) String backofficePassword,
        @NotBlank String fullName,
        @NotBlank String role,
        @NotNull UUID organizationId,
        UUID companyId,
        UUID storeId) {
}