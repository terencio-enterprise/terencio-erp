package es.terencio.erp.catalog.domain.model;

/**
 * Context explaining how a price was determined.
 */
public record PricingContext(
        String source, // TARIFF, CUSTOMER, PROMOTION
        String tariffId,
        String customerId,
        String ruleOrProductId) {
}
