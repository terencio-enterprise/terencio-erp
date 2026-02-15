package es.terencio.erp.sales.application.usecase;

import java.util.UUID;

/**
 * Result of creating a sale draft.
 */
public record CreateSaleDraftResult(
        UUID saleUuid,
        String status) {
}
