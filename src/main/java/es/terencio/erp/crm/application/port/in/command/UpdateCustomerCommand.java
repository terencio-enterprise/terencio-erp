package es.terencio.erp.crm.application.port.in.command;

public record UpdateCustomerCommand(
        String legalName,
        String commercialName,
        String taxId,
        ContactInfoCommand contactInfo,
        BillingInfoCommand billingInfo,
        String notes
) {}