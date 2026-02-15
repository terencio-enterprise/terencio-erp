package es.terencio.erp.catalog.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Command for resolving effective price.
 */
public record ResolveEffectivePriceCommand(
        UUID companyId,
        UUID storeId,
        Long productId,
        Long customerId,
        BigDecimal quantity,
        Instant effectiveDate,
        Long tariffIdOverride) {
}
