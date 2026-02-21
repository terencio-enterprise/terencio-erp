package es.terencio.erp.crm.infrastructure.in.web.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;

public record CustomerResponse(
        UUID uuid,
        String taxId,
        String legalName,
        String commercialName,
        CustomerType type,
        boolean active,
        ContactInfoResponse contactInfo,
        BillingInfoResponse billingInfo,
        MarketingProfileResponse marketingProfile,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static CustomerResponse fromDomain(Customer customer) {
        if (customer == null) return null;

        return new CustomerResponse(
                customer.getUuid(),
                customer.getTaxId() != null ? customer.getTaxId().toString() : null,
                customer.getLegalName(),
                customer.getCommercialName(),
                customer.getType(),
                customer.isActive(),
                ContactInfoResponse.fromDomain(customer.getContactInfo()),
                BillingInfoResponse.fromDomain(customer.getBillingInfo()),
                MarketingProfileResponse.fromDomain(customer.getMarketingProfile()),
                customer.getNotes(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }

    // --- Nested DTOs for strict decoupling of Domain Value Objects ---

    public record ContactInfoResponse(
            String email, 
            String phone, 
            String address, 
            String zipCode, 
            String city, 
            String country
    ) {
        public static ContactInfoResponse fromDomain(es.terencio.erp.crm.domain.model.ContactInfo info) {
            if (info == null) return null;
            return new ContactInfoResponse(
                    info.email() != null ? info.email().value() : null, // Extracting pure string from Email VO
                    info.phone(),
                    info.address(),
                    info.zipCode(),
                    info.city(),
                    info.country()
            );
        }
    }

    public record BillingInfoResponse(
            Long tariffId, 
            boolean allowCredit, 
            Long creditLimitCents, 
            boolean surchargeApply
    ) {
        public static BillingInfoResponse fromDomain(es.terencio.erp.crm.domain.model.BillingInfo info) {
            if (info == null) return null;
            return new BillingInfoResponse(
                    info.tariffId(), 
                    info.allowCredit(), 
                    info.creditLimitCents(), 
                    info.surchargeApply()
            );
        }
    }

    public record MarketingProfileResponse(
            String origin, 
            List<String> tags, 
            boolean consent, 
            String status, 
            Instant lastInteractionAt
    ) {
        public static MarketingProfileResponse fromDomain(es.terencio.erp.crm.domain.model.MarketingProfile info) {
            if (info == null) return null;
            return new MarketingProfileResponse(
                    info.origin(),
                    info.tags(),
                    info.consent(),
                    info.status() != null ? info.status().name() : null,
                    info.lastInteractionAt()
            );
        }
    }
}