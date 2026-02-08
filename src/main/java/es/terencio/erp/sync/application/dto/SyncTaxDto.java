package es.terencio.erp.sync.application.dto;

import java.math.BigDecimal;

public record SyncTaxDto(
        Long id,
        String name,
        BigDecimal rate,
        BigDecimal surcharge,
        String codeAeat,
        boolean active) {}
