package es.terencio.erp.users.application.dto;

import java.time.Instant;

/**
 * DTO representing a user in the system.
 * This is owned by the users module.
 * Other modules (like POS) can import and use it.
 */
public record UserDto(
        Long id, // Backend uses BigSerial, frontend expects number
        String username,
        String fullName,
        String role,
        String pinHash, // We sync the hash so offline login works
        Integer isActive,
        Instant createdAt,
        Instant updatedAt) {
}
