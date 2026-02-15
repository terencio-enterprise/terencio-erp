package es.terencio.erp.catalog.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

import java.time.Instant;

/**
 * ProductPrice entity.
 * Represents a price for a product in a specific tariff.
 */
public class ProductPrice {

    private final ProductId productId;
    private final Long tariffId;
    private Money price;
    private Money costPrice;
    private Instant updatedAt;

    public ProductPrice(
            ProductId productId,
            Long tariffId,
            Money price,
            Money costPrice,
            Instant updatedAt) {

        if (productId == null)
            throw new InvariantViolationException("ProductId cannot be null");
        if (tariffId == null)
            throw new InvariantViolationException("TariffId cannot be null");
        if (price == null)
            throw new InvariantViolationException("Price cannot be null");

        this.productId = productId;
        this.tariffId = tariffId;
        this.price = price;
        this.costPrice = costPrice;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static ProductPrice create(ProductId productId, Long tariffId, Money price) {
        return new ProductPrice(
                productId,
                tariffId,
                price,
                null,
                Instant.now());
    }

    public void updatePrice(Money newPrice) {
        if (newPrice == null)
            throw new InvariantViolationException("Price cannot be null");
        this.price = newPrice;
        this.updatedAt = Instant.now();
    }

    // Getters
    public ProductId productId() {
        return productId;
    }

    public Long tariffId() {
        return tariffId;
    }

    public Money price() {
        return price;
    }

    public Money costPrice() {
        return costPrice;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
