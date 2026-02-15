package es.terencio.erp.catalog.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * Product aggregate root.
 */
public class Product {

    private final ProductId id;
    private final UUID uuid;
    private final CompanyId companyId;
    private String reference;
    private String name;
    private String shortName;
    private String description;
    private Long categoryId;
    private Long taxId;
    private String brand;
    private ProductType type;
    private boolean isWeighted;
    private boolean isInventoriable;
    private BigDecimal minStockAlert;
    private Money averageCost;
    private Money lastPurchaseCost;
    private String imageUrl;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    public Product(
            ProductId id,
            UUID uuid,
            CompanyId companyId,
            String reference,
            String name,
            String shortName,
            String description,
            Long categoryId,
            Long taxId,
            String brand,
            ProductType type,
            boolean isWeighted,
            boolean isInventoriable,
            BigDecimal minStockAlert,
            Money averageCost,
            Money lastPurchaseCost,
            String imageUrl,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {

        // Note: id can be null for new products (will be assigned by persistence)
        if (uuid == null)
            throw new InvariantViolationException("Product UUID cannot be null");
        if (companyId == null)
            throw new InvariantViolationException("Product must belong to a company");
        if (reference == null || reference.isBlank())
            throw new InvariantViolationException("Product reference cannot be empty");
        if (name == null || name.isBlank())
            throw new InvariantViolationException("Product name cannot be empty");
        if (taxId == null)
            throw new InvariantViolationException("Product must have a tax");

        this.id = id;
        this.uuid = uuid;
        this.companyId = companyId;
        this.reference = reference;
        this.name = name;
        this.shortName = shortName;
        this.description = description;
        this.categoryId = categoryId;
        this.taxId = taxId;
        this.brand = brand;
        this.type = type != null ? type : ProductType.PRODUCT;
        this.isWeighted = isWeighted;
        this.isInventoriable = isInventoriable;
        this.minStockAlert = minStockAlert != null ? minStockAlert : BigDecimal.ZERO;
        this.averageCost = averageCost != null ? averageCost : Money.zeroEuros();
        this.lastPurchaseCost = lastPurchaseCost != null ? lastPurchaseCost : Money.zeroEuros();
        this.imageUrl = imageUrl;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.version = version;
    }

    public static Product create(
            CompanyId companyId,
            String reference,
            String name,
            Long taxId,
            ProductType type) {

        return new Product(
                null, // Will be assigned by persistence
                UUID.randomUUID(),
                companyId,
                reference,
                name,
                name.length() > 20 ? name.substring(0, 20) : name,
                null,
                null,
                taxId,
                null,
                type,
                false,
                true,
                BigDecimal.ZERO,
                Money.zeroEuros(),
                Money.zeroEuros(),
                null,
                true,
                Instant.now(),
                Instant.now(),
                1);
    }

    public void updateBasicInfo(String name, String shortName, String description, Long categoryId) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        this.shortName = shortName;
        this.description = description;
        this.categoryId = categoryId;
        this.updatedAt = Instant.now();
    }

    public void updateCost(Money cost) {
        this.lastPurchaseCost = cost;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    // Getters
    public ProductId id() {
        return id;
    }

    public UUID uuid() {
        return uuid;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public String reference() {
        return reference;
    }

    public String name() {
        return name;
    }

    public String shortName() {
        return shortName;
    }

    public String description() {
        return description;
    }

    public Long categoryId() {
        return categoryId;
    }

    public Long taxId() {
        return taxId;
    }

    public String brand() {
        return brand;
    }

    public ProductType type() {
        return type;
    }

    public boolean isWeighted() {
        return isWeighted;
    }

    public boolean isInventoriable() {
        return isInventoriable;
    }

    public BigDecimal minStockAlert() {
        return minStockAlert;
    }

    public Money averageCost() {
        return averageCost;
    }

    public Money lastPurchaseCost() {
        return lastPurchaseCost;
    }

    public String imageUrl() {
        return imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public long version() {
        return version;
    }
}
