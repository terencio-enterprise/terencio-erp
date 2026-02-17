package es.terencio.erp.employees.application.dto;

import java.time.Instant;
import java.util.List;

public record UserDto(
    Long id,
    String username,
    String fullName,
    String role,
    Integer isActive,
    List<String> permissions, 
    Instant createdAt,
    Instant updatedAt
) {}