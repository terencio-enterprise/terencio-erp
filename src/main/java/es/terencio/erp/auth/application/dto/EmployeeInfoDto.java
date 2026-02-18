package es.terencio.erp.auth.application.dto;

/**
 * DTO for current Employee information.
 * Returned by the /me endpoint.
 */
public record EmployeeInfoDto(
                Long id,
                String username,
                String fullName,
                boolean isActive,
                java.util.List<es.terencio.erp.organization.application.dto.CompanyTreeDto> companies) {
}
