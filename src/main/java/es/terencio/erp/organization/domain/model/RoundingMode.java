package es.terencio.erp.organization.domain.model;

/**
 * Rounding mode for price calculations.
 */
public enum RoundingMode {
    LINE, // Round each line item
    TOTAL // Round only the total
}
