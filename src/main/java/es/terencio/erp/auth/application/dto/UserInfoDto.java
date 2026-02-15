package es.terencio.erp.auth.application.dto;

import java.util.UUID;

/**
 * DTO for current user information.
 * Returned by the /me endpoint.
 */
public record UserInfoDto(
        Long id,
        String username,
        String fullName,
        String role,
        UUID storeId,
        boolean isActive) {
}
