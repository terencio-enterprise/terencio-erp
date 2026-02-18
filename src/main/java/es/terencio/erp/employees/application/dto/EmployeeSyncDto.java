package es.terencio.erp.employees.application.dto;

/**
 * DTO for Employee synchronization to POS devices.
 * Includes pinHash for offline PIN verification.
 * Does NOT include backofficePassword for security.
 */
public record EmployeeSyncDto(
                Long id,
                String username,
                String fullName,
                String role,
                String pinHash) {
}
