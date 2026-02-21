package es.terencio.erp.crm.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.TaxId;

public class Customer {

    private final CustomerId id;
    private final UUID uuid;
    private final CompanyId companyId;
    private TaxId taxId;
    private String legalName;
    private String commercialName;
    private Email email;
    private String phone;
    private String address;
    private String zipCode;
    private String city;
    private String country;
    private Long tariffId;
    private boolean allowCredit;
    private Money creditLimit;
    private boolean surchargeApply;
    private String notes;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private CustomerType type;
    private String origin;
    private List<String> tags;
    private boolean marketingConsent;
    private MarketingStatus marketingStatus;
    private String unsubscribeToken;
    private Instant lastInteractionAt;
    private Instant snoozedUntil;

    public Customer(
            CustomerId id, UUID uuid, CompanyId companyId, TaxId taxId, String legalName, String commercialName,
            Email email, String phone, String address, String zipCode, String city, String country,
            Long tariffId, boolean allowCredit, Money creditLimit, boolean surchargeApply, String notes,
            boolean active, Instant createdAt, Instant updatedAt, Instant deletedAt) {

        if (uuid == null)
            throw new InvariantViolationException("Customer UUID cannot be null");
        if (companyId == null)
            throw new InvariantViolationException("Customer must belong to a company");
        if (legalName == null || legalName.isBlank())
            throw new InvariantViolationException("Legal name is required");

        this.id = id;
        this.uuid = uuid;
        this.companyId = companyId;
        this.taxId = taxId;
        this.legalName = legalName;
        this.commercialName = commercialName != null ? commercialName : legalName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.zipCode = zipCode;
        this.city = city;
        this.country = country != null ? country : "ES";
        this.tariffId = tariffId;
        this.allowCredit = allowCredit;
        this.creditLimit = creditLimit != null ? creditLimit : Money.zeroEuros();
        this.surchargeApply = surchargeApply;
        this.notes = notes;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.deletedAt = deletedAt;

        this.type = CustomerType.CLIENT; // Default type
        this.tags = new ArrayList<>();
        this.marketingStatus = MarketingStatus.SUBSCRIBED;
    }

    /**
     * Factory for standard client creation
     */
    public static Customer create(CompanyId companyId, String legalName, TaxId taxId) {
        return new Customer(null, UUID.randomUUID(), companyId, taxId, legalName, legalName, null, null, null, null,
                null, "ES", null, false, Money.zeroEuros(), false, null, true, Instant.now(), Instant.now(), null);
    }

    /**
     * Factory for lead creation
     */
    public static Customer createLead(CompanyId companyId, String name, Email email) {
        Customer lead = create(companyId, name, null);
        lead.email = email;
        lead.type = CustomerType.LEAD;
        lead.marketingStatus = MarketingStatus.SUBSCRIBED;
        return lead;
    }

    public void updateContactInfo(Email email, String phone, String address, String zipCode, String city) {
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.zipCode = zipCode;
        this.city = city;
        this.updatedAt = Instant.now();
    }

    public void applyUnsubscribe() {
        this.marketingStatus = MarketingStatus.UNSUBSCRIBED;
        this.updatedAt = Instant.now();
    }

    public boolean canReceiveMarketing() {
        if (!marketingConsent || !active || deletedAt != null)
            return false;
        if (marketingStatus == MarketingStatus.UNSUBSCRIBED ||
                marketingStatus == MarketingStatus.BOUNCED ||
                marketingStatus == MarketingStatus.COMPLAINED)
            return false;

        return snoozedUntil == null || !snoozedUntil.isAfter(Instant.now());
    }

    // Standard Getters / Domain Setters
    public CustomerId id() {
        return id;
    }

    public UUID uuid() {
        return uuid;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public TaxId taxId() {
        return taxId;
    }

    public String legalName() {
        return legalName;
    }

    public String commercialName() {
        return commercialName;
    }

    public Email email() {
        return email;
    }

    public String phone() {
        return phone;
    }

    public String address() {
        return address;
    }

    public String zipCode() {
        return zipCode;
    }

    public String city() {
        return city;
    }

    public String country() {
        return country;
    }

    public boolean surchargeApply() {
        return surchargeApply;
    }

    public String notes() {
        return notes;
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

    public Instant deletedAt() {
        return deletedAt;
    }

    public CustomerType getType() {
        return type;
    }

    public void setType(CustomerType type) {
        this.type = type;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public void setMarketingConsent(boolean marketingConsent) {
        this.marketingConsent = marketingConsent;
    }

    public MarketingStatus getMarketingStatus() {
        return marketingStatus;
    }

    public void setMarketingStatus(MarketingStatus marketingStatus) {
        this.marketingStatus = marketingStatus;
    }

    public void setUnsubscribeToken(String unsubscribeToken) {
        this.unsubscribeToken = unsubscribeToken;
    }

    public String getUnsubscribeToken() {
        return unsubscribeToken;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(Instant lastInteractionAt) {
        this.lastInteractionAt = lastInteractionAt;
    }

    public void setSnoozedUntil(Instant snoozedUntil) {
        this.snoozedUntil = snoozedUntil;
    }

    // UI Helpers (Records/DTOs)
    public Money creditLimit() {
        return creditLimit;
    }

    public Long tariffId() {
        return tariffId;
    }

    public boolean allowCredit() {
        return allowCredit;
    }

    public void assignTariff(Long tariffId) {
        this.tariffId = tariffId;
        this.updatedAt = Instant.now();
    }

    public void configureCreditSettings(boolean allowCredit, Money creditLimit) {
        this.allowCredit = allowCredit;
        this.creditLimit = creditLimit;
        this.updatedAt = Instant.now();
    }
}