package es.terencio.erp.sync.application.dto;

import java.math.BigDecimal;

public record SyncBarcodeDto(
        String barcode,
        Long productId,
        String type,
        boolean isPrimary,
        BigDecimal quantityFactor) {}
