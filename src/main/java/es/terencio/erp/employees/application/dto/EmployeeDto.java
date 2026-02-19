package es.terencio.erp.employees.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmployeeDto(
                Long id,
                UUID uuid,
                UUID organizationId,
                String username,
                String email,
                String fullName,
                boolean isActive,
                List<String> roles,
                UUID lastActiveCompanyId,
                UUID lastActiveStoreId,
                Instant createdAt,
                Instant updatedAt) {
}