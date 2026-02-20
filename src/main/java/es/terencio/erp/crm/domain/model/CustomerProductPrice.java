package es.terencio.erp.crm.domain.model;

import java.time.Instant;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

public class CustomerProductPrice {

    private final CustomerId customerId;
    private final ProductId productId;
    private Money customPrice;
    private Instant validFrom;
    private Instant validUntil;
    private final Instant createdAt;

    public CustomerProductPrice(
            CustomerId customerId, ProductId productId, Money customPrice,
            Instant validFrom, Instant validUntil, Instant createdAt) {

        if (customerId == null) throw new IllegalArgumentException("CustomerId cannot be null");
        if (productId == null) throw new IllegalArgumentException("ProductId cannot be null");
        if (customPrice == null) throw new IllegalArgumentException("Custom price cannot be null");

        this.customerId = customerId;
        this.productId = productId;
        this.customPrice = customPrice;
        this.validFrom = validFrom != null ? validFrom : Instant.now();
        this.validUntil = validUntil;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static CustomerProductPrice create(CustomerId customerId, ProductId productId, Money customPrice) {
        return new CustomerProductPrice(customerId, productId, customPrice, Instant.now(), null, Instant.now());
    }

    public boolean isValidAt(Instant instant) {
        boolean afterStart = validFrom == null || !instant.isBefore(validFrom);
        boolean beforeEnd = validUntil == null || !instant.isAfter(validUntil);
        return afterStart && beforeEnd;
    }

    public CustomerId customerId() { return customerId; }
    public ProductId productId() { return productId; }
    public Money customPrice() { return customPrice; }
    public Instant validFrom() { return validFrom; }
    public Instant validUntil() { return validUntil; }
    public Instant createdAt() { return createdAt; }
}