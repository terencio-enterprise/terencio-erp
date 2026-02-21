package es.terencio.erp.crm.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import es.terencio.erp.crm.domain.model.valueobject.CommercialSettings;
import es.terencio.erp.crm.domain.model.valueobject.ContactInfo;
import es.terencio.erp.crm.domain.model.valueobject.MarketingProfile;
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

    // Value Objects for specific concerns
    private ContactInfo contactInfo;
    private CommercialSettings commercialSettings;
    private MarketingProfile marketingProfile;

    // Metadata
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    /**
     * Domain Logic: Create a new Lead
     */
    public static Customer createLead(CompanyId companyId, String name, Email email, String origin, List<String> tags) {
        return Customer.builder()
                .uuid(UUID.randomUUID())
                .companyId(companyId)
                .legalName(name)
                .commercialName(name)
                .type(CustomerType.LEAD)
                .active(true)
                .contactInfo(ContactInfo.builder().email(email).build())
                .marketingProfile(MarketingProfile.createDefault(origin, tags))
                .commercialSettings(CommercialSettings.defaultSettings())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    /**
     * Domain Logic: Create a standard Client
     */
    public static Customer createClient(CompanyId companyId, String legalName, TaxId taxId) {
        if (legalName == null || legalName.isBlank())
            throw new InvariantViolationException("Legal name required");

        return Customer.builder()
                .uuid(UUID.randomUUID())
                .companyId(companyId)
                .legalName(legalName)
                .commercialName(legalName)
                .taxId(taxId)
                .type(CustomerType.CLIENT)
                .active(true)
                .contactInfo(ContactInfo.empty())
                .commercialSettings(CommercialSettings.defaultSettings())
                .marketingProfile(MarketingProfile.empty())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public void updateContact(ContactInfo newInfo) {
        this.contactInfo = newInfo;
        this.updatedAt = Instant.now();
    }

    public void updateCommercialTerms(CommercialSettings settings) {
        this.commercialSettings = settings;
        this.updatedAt = Instant.now();
    }

    public void unsubscribe() {
        this.marketingProfile = this.marketingProfile.withStatus(MarketingStatus.UNSUBSCRIBED);
        this.updatedAt = Instant.now();
    }

    public boolean canReceiveMarketing() {
        return active && deletedAt == null && marketingProfile.isEligible();
    }
}