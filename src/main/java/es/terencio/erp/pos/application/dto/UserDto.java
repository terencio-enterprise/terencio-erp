package es.terencio.erp.pos.application.dto;

import java.time.Instant;

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