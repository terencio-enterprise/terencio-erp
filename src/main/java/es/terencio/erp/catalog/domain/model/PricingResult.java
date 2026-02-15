package es.terencio.erp.catalog.domain.model;

import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.Quantity;

/**
 * Value Object representing effective price resolution result.
 * Contains the final unit price and the context explaining how it was
 * determined.
 */
public record PricingResult(
        Money unitPrice,
        PricingContext context) {

    public Money calculateLineTotal(Quantity quantity) {
        return unitPrice.multiply(quantity.value());
    }

    public static PricingResult fromTariff(Money price, Long tariffId) {
        return new PricingResult(
                price,
                new PricingContext("TARIFF", tariffId.toString(), null, null));
    }

    public static PricingResult fromCustomerPrice(Money price, CustomerId customerId, ProductId productId) {
        return new PricingResult(
                price,
                new PricingContext("CUSTOMER", null, customerId.value().toString(), productId.value().toString()));
    }

    public static PricingResult fromPromotion(Money price, Long ruleId) {
        return new PricingResult(
                price,
                new PricingContext("PROMOTION", null, null, ruleId.toString()));
    }
}
