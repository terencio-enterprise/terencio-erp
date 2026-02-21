package es.terencio.erp.crm.infrastructure.in.web.dto;

import es.terencio.erp.crm.application.port.in.command.BillingInfoCommand;
import es.terencio.erp.crm.application.port.in.command.ContactInfoCommand;
import es.terencio.erp.crm.application.port.in.command.UpdateCustomerCommand;

public record UpdateCustomerRequest(
        String legalName,
        String commercialName,
        String taxId,
        ContactInfoRequest contactInfo,
        BillingInfoRequest billingInfo,
        String notes
) {
    public UpdateCustomerCommand toCommand() {
        return new UpdateCustomerCommand(
                legalName,
                commercialName,
                taxId,
                contactInfo != null ? contactInfo.toCommand() : null,
                billingInfo != null ? billingInfo.toCommand() : null,
                notes
        );
    }

    public record ContactInfoRequest(
            String email,
            String phone,
            String address,
            String zipCode,
            String city,
            String country
    ) {
        public ContactInfoCommand toCommand() {
            return new ContactInfoCommand(email, phone, address, zipCode, city, country);
        }
    }

    public record BillingInfoRequest(
            Long tariffId,
            Boolean allowCredit,
            Long creditLimitCents,
            Boolean surchargeApply
    ) {
        public BillingInfoCommand toCommand() {
            return new BillingInfoCommand(tariffId, allowCredit, creditLimitCents, surchargeApply);
        }
    }
}