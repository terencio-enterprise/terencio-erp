package es.terencio.erp.catalog.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;

/**
 * Tariff entity.
 * Represents a pricing schedule (e.g., retail, wholesale).
 */
public class Tariff {

    private final Long id;
    private final CompanyId companyId;
    private String name;
    private int priority;
    private String priceType; // RETAIL, WHOLESALE
    private boolean isDefault;
    private boolean active;
    private long version;

    public Tariff(
            Long id,
            CompanyId companyId,
            String name,
            int priority,
            String priceType,
            boolean isDefault,
            boolean active,
            long version) {

        if (companyId == null)
            throw new InvariantViolationException("Tariff must belong to a company");
        if (name == null || name.isBlank())
            throw new InvariantViolationException("Tariff name cannot be empty");

        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.priority = priority;
        this.priceType = priceType != null ? priceType : "RETAIL";
        this.isDefault = isDefault;
        this.active = active;
        this.version = version;
    }

    public static Tariff create(CompanyId companyId, String name, boolean isDefault) {
        return new Tariff(
                null,
                companyId,
                name,
                0,
                "RETAIL",
                isDefault,
                true,
                1);
    }

    // Getters
    public Long id() {
        return id;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public String name() {
        return name;
    }

    public int priority() {
        return priority;
    }

    public String priceType() {
        return priceType;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isActive() {
        return active;
    }

    public long version() {
        return version;
    }
}
