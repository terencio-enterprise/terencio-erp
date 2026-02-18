package es.terencio.erp.auth.application.dto;

import java.util.Set;
import java.util.UUID;

import es.terencio.erp.auth.domain.model.AccessGrant;

/**
 * DTO for current Employee information.
 * Returned by the /me endpoint.
 */
public record EmployeeInfoDto(
                Long id,
                String username,
                String fullName,
                String role,
                UUID organizationId,
                UUID companyId,
                UUID storeId,
                Set<AccessGrant> grants,
                boolean isActive) {
}
