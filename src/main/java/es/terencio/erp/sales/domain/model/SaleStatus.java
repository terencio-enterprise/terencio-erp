package es.terencio.erp.sales.domain.model;

/**
 * Sale status lifecycle.
 */
public enum SaleStatus {
    DRAFT, // Being built
    ISSUED, // Finalized and numbered
    FISCALIZED, // Sent to fiscal audit log
    CANCELLED // Cancelled (requires credit note)
}
