package es.terencio.erp.crm.application.port.in.command;

import es.terencio.erp.crm.domain.model.CustomerType;

public record CreateCustomerCommand(
        String legalName,
        String taxId,
        CustomerType type,
        String email,
        String phone
) {}