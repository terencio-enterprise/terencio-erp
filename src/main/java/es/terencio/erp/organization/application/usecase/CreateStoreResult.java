package es.terencio.erp.organization.application.usecase;

import java.util.UUID;

/**
 * Result of store creation.
 */
public record CreateStoreResult(
        UUID storeId,
        UUID warehouseId,
        String storeCode,
        String storeName) {
}
