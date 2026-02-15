package es.terencio.erp.inventory.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;
import es.terencio.erp.shared.domain.valueobject.Quantity;

import java.time.Instant;

/**
 * InventoryStock entity (snapshot).
 * Represents current stock quantity for a product in a warehouse.
 */
public class InventoryStock {

    private final ProductId productId;
    private final WarehouseId warehouseId;
    private final CompanyId companyId;
    private Quantity quantityOnHand;
    private Instant lastUpdatedAt;
    private long version;

    public InventoryStock(
            ProductId productId,
            WarehouseId warehouseId,
            CompanyId companyId,
            Quantity quantityOnHand,
            Instant lastUpdatedAt,
            long version) {

        if (productId == null)
            throw new InvariantViolationException("ProductId cannot be null");
        if (warehouseId == null)
            throw new InvariantViolationException("WarehouseId cannot be null");
        if (companyId == null)
            throw new InvariantViolationException("CompanyId cannot be null");
        if (quantityOnHand == null)
            throw new InvariantViolationException("Quantity cannot be null");

        this.productId = productId;
        this.warehouseId = warehouseId;
        this.companyId = companyId;
        this.quantityOnHand = quantityOnHand;
        this.lastUpdatedAt = lastUpdatedAt != null ? lastUpdatedAt : Instant.now();
        this.version = version;
    }

    public static InventoryStock initialize(
            ProductId productId,
            WarehouseId warehouseId,
            CompanyId companyId,
            Quantity initialQuantity) {

        return new InventoryStock(
                productId,
                warehouseId,
                companyId,
                initialQuantity,
                Instant.now(),
                1);
    }

    public void adjustQuantity(Quantity delta) {
        this.quantityOnHand = quantityOnHand.add(delta);
        this.lastUpdatedAt = Instant.now();
    }

    public boolean hasAvailableStock(Quantity required) {
        return quantityOnHand.isGreaterThanOrEqual(required);
    }

    // Getters
    public ProductId productId() {
        return productId;
    }

    public WarehouseId warehouseId() {
        return warehouseId;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public Quantity quantityOnHand() {
        return quantityOnHand;
    }

    public Instant lastUpdatedAt() {
        return lastUpdatedAt;
    }

    public long version() {
        return version;
    }
}
