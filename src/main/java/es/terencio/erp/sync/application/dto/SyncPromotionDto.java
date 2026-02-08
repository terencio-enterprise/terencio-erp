package es.terencio.erp.sync.application.dto;

import java.time.Instant;

public record SyncPromotionDto(
        Long id,
        String name,
        String type,
        Instant startDate,
        Instant endDate,
        int priority,
        String rulesJson,
        boolean active) {}
