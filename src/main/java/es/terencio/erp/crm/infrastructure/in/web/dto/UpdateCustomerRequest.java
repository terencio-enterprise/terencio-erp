package es.terencio.erp.crm.infrastructure.in.web.dto;

import es.terencio.erp.crm.application.port.in.command.UpdateCustomerCommand;
import es.terencio.erp.crm.domain.model.BillingInfo;
import es.terencio.erp.crm.domain.model.ContactInfo;

public record UpdateCustomerRequest(
        String legalName,
        String commercialName,
        String taxId,
        ContactInfo contactInfo,
        BillingInfo billingInfo,
        String notes
) {
    public UpdateCustomerCommand toCommand() {
        return new UpdateCustomerCommand(legalName, commercialName, taxId, contactInfo, billingInfo, notes);
    }
}