package es.terencio.erp.employees.application.dto;

import java.time.Instant;
import java.util.UUID;

public record EmployeeDto(
                Long id,
                String username,
                String fullName,
                String role,
                Integer isActive,
                String permissionsJson,
                UUID lastActiveCompanyId,
                UUID lastActiveStoreId,
                Instant createdAt,
                Instant updatedAt) {
}
