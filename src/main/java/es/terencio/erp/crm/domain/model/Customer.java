package es.terencio.erp.crm.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.TaxId;

/**
 * Customer aggregate root.
 */
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

    // Marketing Fields
    private String type; // LEAD, CLIENT, PROSPECT
    private String origin;
    private String[] tags;
    private boolean marketingConsent;
    private String marketingStatus;
    private String unsubscribeToken;
    private Instant lastInteractionAt;
    private Instant snoozedUntil;

    public Customer(
            CustomerId id,
            UUID uuid,
            CompanyId companyId,
            TaxId taxId,
            String legalName,
            String commercialName,
            Email email,
            String phone,
            String address,
            String zipCode,
            String city,
            String country,
            Long tariffId,
            boolean allowCredit,
            Money creditLimit,
            boolean surchargeApply,
            String notes,
            boolean active,
            Instant createdAt,
            Instant updatedAt) {

        if (uuid == null)
            throw new InvariantViolationException("Customer UUID cannot be null");
        if (companyId == null)
            throw new InvariantViolationException("Customer must belong to a company");

        this.id = id;
        this.uuid = uuid;
        this.companyId = companyId;
        this.taxId = taxId;
        this.legalName = legalName;
        this.commercialName = commercialName;
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
    }

    public static Customer create(CompanyId companyId, String legalName, TaxId taxId) {
        return new Customer(
                null,
                UUID.randomUUID(),
                companyId,
                taxId,
                legalName,
                legalName,
                null,
                null,
                null,
                null,
                null,
                "ES",
                null,
                false,
                Money.zeroEuros(),
                false,
                null,
                true,
                Instant.now(),
                Instant.now());
    }

    public void updateContactInfo(Email email, String phone, String address, String zipCode, String city) {
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.zipCode = zipCode;
        this.city = city;
        this.updatedAt = Instant.now();
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

    // Getters
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

    public Long tariffId() {
        return tariffId;
    }

    public boolean allowCredit() {
        return allowCredit;
    }

    public Money creditLimit() {
        return creditLimit;
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

    // Marketing Accessors

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public boolean isMarketingConsent() {
        return marketingConsent;
    }

    public void setMarketingConsent(boolean consent) {
        this.marketingConsent = consent;
        this.updatedAt = Instant.now();
    }

    public String getMarketingStatus() {
        return marketingStatus;
    }

    public void setMarketingStatus(String status) {
        this.marketingStatus = status;
        this.updatedAt = Instant.now();
    }

    public String getUnsubscribeToken() {
        return unsubscribeToken;
    }

    public void setUnsubscribeToken(String token) {
        this.unsubscribeToken = token;
    }

    public Instant getLastInteractionAt() {
        return lastInteractionAt;
    }

    public void setLastInteractionAt(Instant at) {
        this.lastInteractionAt = at;
    }

    public Instant getSnoozedUntil() {
        return snoozedUntil;
    }

    public void setSnoozedUntil(Instant until) {
        this.snoozedUntil = until;
        this.updatedAt = Instant.now();
    }

    public boolean canReceiveMarketing() {
        if (!marketingConsent)
            return false;
        if ("UNSUBSCRIBED".equals(marketingStatus))
            return false;
        if ("BOUNCED".equals(marketingStatus))
            return false;
        if (snoozedUntil != null && snoozedUntil.isAfter(Instant.now()))
            return false;
        return true;
    }
}
