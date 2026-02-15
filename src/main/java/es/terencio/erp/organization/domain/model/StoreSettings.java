package es.terencio.erp.organization.domain.model;

import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.valueobject.Money;

import java.time.Instant;

/**
 * Store Settings entity (1:1 with Store).
 * Configuration specific to a store's operational behavior.
 */
public class StoreSettings {

    private final StoreId storeId;
    private boolean allowNegativeStock;
    private Long defaultTariffId; // Reference to Tariff (kept as Long to avoid circular dependency)
    private boolean printTicketAutomatically;
    private Money requireCustomerForLargeAmount;
    private Instant updatedAt;

    public StoreSettings(
            StoreId storeId,
            boolean allowNegativeStock,
            Long defaultTariffId,
            boolean printTicketAutomatically,
            Money requireCustomerForLargeAmount,
            Instant updatedAt) {

        if (storeId == null)
            throw new IllegalArgumentException("StoreId cannot be null");

        this.storeId = storeId;
        this.allowNegativeStock = allowNegativeStock;
        this.defaultTariffId = defaultTariffId;
        this.printTicketAutomatically = printTicketAutomatically;
        this.requireCustomerForLargeAmount = requireCustomerForLargeAmount;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static StoreSettings createDefault(StoreId storeId) {
        return new StoreSettings(
                storeId,
                false,
                null,
                true,
                Money.ofEuros(1000.0),
                Instant.now());
    }

    public void updateSettings(
            boolean allowNegativeStock,
            Long defaultTariffId,
            boolean printTicketAutomatically,
            Money requireCustomerForLargeAmount) {

        this.allowNegativeStock = allowNegativeStock;
        this.defaultTariffId = defaultTariffId;
        this.printTicketAutomatically = printTicketAutomatically;
        this.requireCustomerForLargeAmount = requireCustomerForLargeAmount;
        this.updatedAt = Instant.now();
    }

    // Getters
    public StoreId storeId() {
        return storeId;
    }

    public boolean allowNegativeStock() {
        return allowNegativeStock;
    }

    public Long defaultTariffId() {
        return defaultTariffId;
    }

    public boolean printTicketAutomatically() {
        return printTicketAutomatically;
    }

    public Money requireCustomerForLargeAmount() {
        return requireCustomerForLargeAmount;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
