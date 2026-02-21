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

    private TaxId taxId;
    private String legalName;
    private String commercialName;
    private CustomerType type;
    private boolean active;

    private ContactInfo contactInfo;
    private BillingInfo billingInfo;
    private MarketingProfile marketingProfile;

    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    // =========================================================
    // FACTORIES
    // =========================================================

    public static Customer newLead(
            CompanyId companyId,
            String name,
            Email email,
            String phone,
            String origin,
            List<String> tags,
            boolean consent
    ) {
        if (email == null)
            throw new InvariantViolationException("Lead must have an email");

        if (name == null || name.isBlank())
            throw new InvariantViolationException("Lead must have a name");

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

    public static Customer newClient(
            CompanyId companyId,
            String legalName,
            TaxId taxId,
            CustomerType type
    ) {
        if (legalName == null || legalName.isBlank())
            throw new InvariantViolationException("Client must have legal name");

        CustomerType resolved = type != null ? type : CustomerType.CLIENT_RETAIL;

        if (resolved == CustomerType.LEAD)
            throw new InvariantViolationException("Client cannot be LEAD");

        return Customer.builder()
                .uuid(UUID.randomUUID())
                .companyId(companyId)
                .legalName(legalName)
                .commercialName(legalName)
                .taxId(taxId)
                .type(resolved)
                .active(true)
                .contactInfo(ContactInfo.empty())
                .billingInfo(BillingInfo.defaultSettings())
                .marketingProfile(MarketingProfile.empty())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // =========================================================
    // DOMAIN BEHAVIORS
    // =========================================================

    public void rename(String newLegalName, String newCommercialName) {
        if (newLegalName != null) {
            if (newLegalName.isBlank())
                throw new InvariantViolationException("legalName cannot be blank");
            this.legalName = newLegalName;
        }

        if (newCommercialName != null) {
            this.commercialName = newCommercialName;
        }

        touch();
    }

    public void changeTaxId(TaxId newTaxId) {
        if (this.type == CustomerType.LEAD)
            throw new InvariantViolationException("Leads cannot have taxId");

        this.taxId = newTaxId;
        touch();
    }

    public void updateNotes(String notes) {
        if (notes != null) {
            this.notes = notes;
            touch();
        }
    }

    public void updateContactInfo(ContactInfo info) {
        if (info != null) {
            this.contactInfo = info;
            touch();
        }
    }

    public void updateBillingInfo(BillingInfo info) {
        if (info != null) {
            this.billingInfo = info;
            touch();
        }
    }

    public void convertToClient(CustomerType newType, TaxId taxId) {
        if (this.type != CustomerType.LEAD)
            throw new InvariantViolationException("Only LEAD can be converted");

        if (newType == null || newType == CustomerType.LEAD)
            throw new InvariantViolationException("Invalid client type");

        this.type = newType;
        this.taxId = taxId;
        touch();
    }

    public void deactivate() {
        this.active = false;
        this.deletedAt = Instant.now();
        touch();
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}