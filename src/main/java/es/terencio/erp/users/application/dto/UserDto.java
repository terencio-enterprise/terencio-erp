package es.terencio.erp.users.application.dto;

import java.time.Instant;
import java.util.List;

public record UserDto(
    Long id,
    String username,
    String fullName,
    String role,
    Integer isActive,
    List<String> permissions, // Granular permissions
    Instant createdAt,
    Instant updatedAt
) {}