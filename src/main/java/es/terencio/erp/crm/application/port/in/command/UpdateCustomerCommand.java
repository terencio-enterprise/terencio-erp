package es.terencio.erp.crm.application.port.in.command;

import es.terencio.erp.crm.domain.model.BillingInfo;
import es.terencio.erp.crm.domain.model.ContactInfo;

public record UpdateCustomerCommand(
        String legalName,
        String commercialName,
        String taxId,
        ContactInfo contactInfo,
        BillingInfo billingInfo,
        String notes
) {}