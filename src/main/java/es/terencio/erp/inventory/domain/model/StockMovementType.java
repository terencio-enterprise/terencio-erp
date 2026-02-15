package es.terencio.erp.inventory.domain.model;

/**
 * Type of stock movement.
 */
public enum StockMovementType {
    SALE, // Product sold (reduces stock)
    RETURN, // Product returned (increases stock)
    ADJUSTMENT // Manual stock adjustment
}
