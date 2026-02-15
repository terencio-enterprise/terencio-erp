package es.terencio.erp.sales.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.SaleId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * Payment entity.
 * Represents a payment for a sale.
 */
public class Payment {

    private final Long id;
    private final UUID uuid;
    private final SaleId saleUuid;
    private final Long paymentMethodId;
    private final Money amount;
    private final String currency;
    private final Instant createdAtPos;

    public Payment(
            Long id,
            UUID uuid,
            SaleId saleUuid,
            Long paymentMethodId,
            Money amount,
            String currency,
            Instant createdAtPos) {

        if (uuid == null)
            throw new InvariantViolationException("Payment UUID cannot be null");
        if (saleUuid == null)
            throw new InvariantViolationException("Sale UUID cannot be null");
        if (paymentMethodId == null)
            throw new InvariantViolationException("Payment method cannot be null");
        if (amount == null || !amount.isPositive())
            throw new InvariantViolationException("Payment amount must be positive");

        this.id = id;
        this.uuid = uuid;
        this.saleUuid = saleUuid;
        this.paymentMethodId = paymentMethodId;
        this.amount = amount;
        this.currency = currency != null ? currency : "EUR";
        this.createdAtPos = createdAtPos != null ? createdAtPos : Instant.now();
    }

    public static Payment create(SaleId saleUuid, Long paymentMethodId, Money amount) {
        return new Payment(
                null,
                UUID.randomUUID(),
                saleUuid,
                paymentMethodId,
                amount,
                "EUR",
                Instant.now());
    }

    // Getters
    public Long id() {
        return id;
    }

    public UUID uuid() {
        return uuid;
    }

    public SaleId saleUuid() {
        return saleUuid;
    }

    public Long paymentMethodId() {
        return paymentMethodId;
    }

    public Money amount() {
        return amount;
    }

    public String currency() {
        return currency;
    }

    public Instant createdAtPos() {
        return createdAtPos;
    }
}
