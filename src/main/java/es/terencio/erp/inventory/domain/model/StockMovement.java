package es.terencio.erp.inventory.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.SaleId;
import es.terencio.erp.shared.domain.identifier.UserId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.Quantity;

import java.time.Instant;
import java.util.UUID;

/**
 * StockMovement entity (source of truth for inventory changes).
 */
public class StockMovement {

    private final Long id;
    private final UUID uuid;
    private final ProductId productId;
    private final WarehouseId warehouseId;
    private final StockMovementType type;
    private final Quantity quantity;
    private final Quantity previousBalance;
    private final Quantity newBalance;
    private final Money costUnit;
    private final String reason;
    private final String referenceDocType;
    private final SaleId referenceDocUuid;
    private final UserId userId;
    private final Instant createdAt;

    public StockMovement(
            Long id,
            UUID uuid,
            ProductId productId,
            WarehouseId warehouseId,
            StockMovementType type,
            Quantity quantity,
            Quantity previousBalance,
            Quantity newBalance,
            Money costUnit,
            String reason,
            String referenceDocType,
            SaleId referenceDocUuid,
            UserId userId,
            Instant createdAt) {

        if (uuid == null)
            throw new InvariantViolationException("Movement UUID cannot be null");
        if (productId == null)
            throw new InvariantViolationException("ProductId cannot be null");
        if (warehouseId == null)
            throw new InvariantViolationException("WarehouseId cannot be null");
        if (type == null)
            throw new InvariantViolationException("Movement type cannot be null");
        if (quantity == null)
            throw new InvariantViolationException("Quantity cannot be null");
        if (previousBalance == null)
            throw new InvariantViolationException("Previous balance cannot be null");
        if (newBalance == null)
            throw new InvariantViolationException("New balance cannot be null");

        this.id = id;
        this.uuid = uuid;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.type = type;
        this.quantity = quantity;
        this.previousBalance = previousBalance;
        this.newBalance = newBalance;
        this.costUnit = costUnit;
        this.reason = reason;
        this.referenceDocType = referenceDocType;
        this.referenceDocUuid = referenceDocUuid;
        this.userId = userId;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static StockMovement forSale(
            ProductId productId,
            WarehouseId warehouseId,
            Quantity quantitySold,
            Quantity previousBalance,
            Money costUnit,
            SaleId saleId,
            UserId userId) {

        Quantity delta = quantitySold.negate();
        Quantity newBalance = previousBalance.add(delta);

        return new StockMovement(
                null,
                UUID.randomUUID(),
                productId,
                warehouseId,
                StockMovementType.SALE,
                delta,
                previousBalance,
                newBalance,
                costUnit,
                "Sale",
                "SALE",
                saleId,
                userId,
                Instant.now());
    }

    public static StockMovement forAdjustment(
            ProductId productId,
            WarehouseId warehouseId,
            Quantity adjustmentQty,
            Quantity previousBalance,
            String reason,
            UserId userId) {

        Quantity newBalance = previousBalance.add(adjustmentQty);

        return new StockMovement(
                null,
                UUID.randomUUID(),
                productId,
                warehouseId,
                StockMovementType.ADJUSTMENT,
                adjustmentQty,
                previousBalance,
                newBalance,
                null,
                reason,
                "ADJUSTMENT",
                null,
                userId,
                Instant.now());
    }

    // Getters
    public Long id() {
        return id;
    }

    public UUID uuid() {
        return uuid;
    }

    public ProductId productId() {
        return productId;
    }

    public WarehouseId warehouseId() {
        return warehouseId;
    }

    public StockMovementType type() {
        return type;
    }

    public Quantity quantity() {
        return quantity;
    }

    public Quantity previousBalance() {
        return previousBalance;
    }

    public Quantity newBalance() {
        return newBalance;
    }

    public Money costUnit() {
        return costUnit;
    }

    public String reason() {
        return reason;
    }

    public String referenceDocType() {
        return referenceDocType;
    }

    public SaleId referenceDocUuid() {
        return referenceDocUuid;
    }

    public UserId userId() {
        return userId;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
