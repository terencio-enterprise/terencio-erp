package es.terencio.erp.organization.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.TaxId;

import java.time.Instant;
import java.util.Currency;

/**
 * Company aggregate root.
 * Represents a legal entity that can own multiple stores.
 */
public class Company {

    private final CompanyId id;
    private String name;
    private TaxId taxId;
    private Currency currency;
    private FiscalRegime fiscalRegime;
    private boolean priceIncludesTax;
    private RoundingMode roundingMode;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    // Constructor for reconstitution from persistence
    public Company(
            CompanyId id,
            String name,
            TaxId taxId,
            Currency currency,
            FiscalRegime fiscalRegime,
            boolean priceIncludesTax,
            RoundingMode roundingMode,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            long version) {

        if (id == null)
            throw new InvariantViolationException("Company ID cannot be null");
        if (name == null || name.isBlank())
            throw new InvariantViolationException("Company name cannot be empty");
        if (taxId == null)
            throw new InvariantViolationException("Company tax ID cannot be null");

        this.id = id;
        this.name = name;
        this.taxId = taxId;
        this.currency = currency != null ? currency : Currency.getInstance("EUR");
        this.fiscalRegime = fiscalRegime != null ? fiscalRegime : FiscalRegime.COMMON;
        this.priceIncludesTax = priceIncludesTax;
        this.roundingMode = roundingMode != null ? roundingMode : RoundingMode.LINE;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.version = version;
    }

    // Factory method for new company creation
    public static Company create(String name, String taxIdValue, String currencyCode) {
        return new Company(
                CompanyId.create(),
                name,
                TaxId.of(taxIdValue),
                Currency.getInstance(currencyCode),
                FiscalRegime.COMMON,
                true,
                RoundingMode.LINE,
                true,
                Instant.now(),
                Instant.now(),
                1);
    }

    public void updateName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new InvariantViolationException("Company name cannot be empty");
        }
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public void updateTaxId(TaxId newTaxId) {
        if (newTaxId == null) {
            throw new InvariantViolationException("Company tax ID cannot be null");
        }
        this.taxId = newTaxId;
        this.updatedAt = Instant.now();
    }

    public void configureFiscalSettings(FiscalRegime regime, boolean priceIncludesTax, RoundingMode rounding) {
        this.fiscalRegime = regime;
        this.priceIncludesTax = priceIncludesTax;
        this.roundingMode = rounding;
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
    public CompanyId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public TaxId taxId() {
        return taxId;
    }

    public Currency currency() {
        return currency;
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }

    public FiscalRegime fiscalRegime() {
        return fiscalRegime;
    }

    public boolean priceIncludesTax() {
        return priceIncludesTax;
    }

    public RoundingMode roundingMode() {
        return roundingMode;
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
