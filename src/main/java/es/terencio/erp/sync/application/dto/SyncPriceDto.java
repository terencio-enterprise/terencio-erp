package es.terencio.erp.sync.application.dto;

import java.math.BigDecimal;

public record SyncPriceDto(
        Long productId,
        Long tariffId,
        BigDecimal price) {}
