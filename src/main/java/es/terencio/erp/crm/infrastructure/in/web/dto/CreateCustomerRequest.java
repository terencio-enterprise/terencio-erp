package es.terencio.erp.crm.infrastructure.in.web.dto;

import es.terencio.erp.crm.application.port.in.command.CreateCustomerCommand;
import es.terencio.erp.crm.domain.model.CustomerType;
import jakarta.validation.constraints.NotBlank;

public record CreateCustomerRequest(
        @NotBlank String legalName,
        String taxId,
        CustomerType type,
        String email,
        String phone
) {
    public CreateCustomerCommand toCommand() {
        return new CreateCustomerCommand(legalName, taxId, type, email, phone);
    }
}