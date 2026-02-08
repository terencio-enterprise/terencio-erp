package es.terencio.erp.sync.application.dto;

import java.time.Instant;

public record SyncUserDto(
        Long id,
        String username,
        String pinHash,
        String fullName,
        String role,
        String permissionsJson,
        boolean isActive,
        Instant updatedAt) {}
