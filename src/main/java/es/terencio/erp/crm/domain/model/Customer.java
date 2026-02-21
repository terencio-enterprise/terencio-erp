package es.terencio.erp.crm.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.TaxId;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Customer {

    private final CustomerId id;
    private final UUID uuid;
    private final CompanyId companyId;

    // Core Identity
    private TaxId taxId;
    private String legalName;
    private String commercialName;
    private CustomerType type;
    private boolean active;

    // Immutable Value Objects
    private ContactInfo contactInfo;
    private BillingInfo billingInfo;
    private MarketingProfile marketingProfile;

    // Metadata
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    // --- Domain Behaviors (Factory Methods) ---

    public static Customer newLead(CompanyId companyId, String name, Email email, String phone, String origin, List<String> tags, boolean consent) {
        if (email == null) throw new InvariantViolationException("Lead must have an email");
        
        return Customer.builder()
                .uuid(UUID.randomUUID())
                .companyId(companyId)
                .legalName(name)
                .commercialName(name)
                .type(CustomerType.LEAD)
                .active(true)
                .contactInfo(new ContactInfo(email, phone, null, null, null, "ES"))
                .billingInfo(BillingInfo.defaultSettings())
                .marketingProfile(MarketingProfile.createLead(origin, tags, consent))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static Customer newClient(CompanyId companyId, String legalName, TaxId taxId, CustomerType type) {
        if (legalName == null || legalName.isBlank()) throw new InvariantViolationException("Client must have a legal name");
        
        return Customer.builder()
                .uuid(UUID.randomUUID())
                .companyId(companyId)
                .legalName(legalName)
                .commercialName(legalName)
                .taxId(taxId)
                .type(type != null ? type : CustomerType.CLIENT_RETAIL)
                .active(true)
                .contactInfo(ContactInfo.empty())
                .billingInfo(BillingInfo.defaultSettings())
                .marketingProfile(MarketingProfile.empty())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // --- Domain Mutations ---

    public void updateDetails(String legalName, String commercialName, TaxId taxId, String notes) {
        this.legalName = legalName;
        this.commercialName = commercialName;
        this.taxId = taxId;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public void updateContactInfo(ContactInfo info) {
        this.contactInfo = info;
        this.updatedAt = Instant.now();
    }

    public void updateBillingInfo(BillingInfo info) {
        this.billingInfo = info;
        this.updatedAt = Instant.now();
    }

    public void markAsDeleted() {
        this.active = false;
        this.deletedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}