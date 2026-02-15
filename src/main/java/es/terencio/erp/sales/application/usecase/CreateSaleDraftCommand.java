package es.terencio.erp.sales.application.usecase;

import java.util.UUID;

/**
 * Command for creating a sale draft.
 */
public record CreateSaleDraftCommand(
        UUID companyId,
        UUID storeId,
        UUID deviceId,
        Long userId,
        String documentType,
        Long customerId) {
}
