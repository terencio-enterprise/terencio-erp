package es.terencio.erp.sync.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SyncProductDto(
        Long id,
        UUID uuid,
        String reference,
        String name,
        String shortName,
        String description,
        String familyCode,
        Long taxId,
        boolean isWeighted,
        boolean isService,
        boolean isAgeRestricted,
        boolean requiresManager,
        boolean stockTracking,
        BigDecimal minStockAlert,
        String imageUrl,
        boolean active, // Used for soft-delete sync
        Instant updatedAt) {}
