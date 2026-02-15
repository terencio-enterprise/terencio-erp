package es.terencio.erp.catalog.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.TaxRate;

import java.time.Instant;

/**
 * Tax entity.
 * Represents VAT/IGIC rates.
 */
public class Tax {

    private final Long id;
    private final CompanyId companyId;
    private String name;
    private TaxRate rate;
    private TaxRate surcharge;
    private String codeAeat;
    private boolean active;
    private final Instant createdAt;

    public Tax(
            Long id,
            CompanyId companyId,
            String name,
            TaxRate rate,
            TaxRate surcharge,
            String codeAeat,
            boolean active,
            Instant createdAt) {

        if (companyId == null)
            throw new InvariantViolationException("Tax must belong to a company");
        if (name == null || name.isBlank())
            throw new InvariantViolationException("Tax name cannot be empty");
        if (rate == null)
            throw new InvariantViolationException("Tax rate cannot be null");

        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.rate = rate;
        this.surcharge = surcharge != null ? surcharge : TaxRate.zero();
        this.codeAeat = codeAeat;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Tax create(CompanyId companyId, String name, TaxRate rate, String codeAeat) {
        return new Tax(
                null,
                companyId,
                name,
                rate,
                TaxRate.zero(),
                codeAeat,
                true,
                Instant.now());
    }

    public void updateRate(TaxRate newRate) {
        if (newRate == null)
            throw new InvariantViolationException("Tax rate cannot be null");
        this.rate = newRate;
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

    public TaxRate rate() {
        return rate;
    }

    public TaxRate surcharge() {
        return surcharge;
    }

    public String codeAeat() {
        return codeAeat;
    }

    public boolean isActive() {
        return active;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
