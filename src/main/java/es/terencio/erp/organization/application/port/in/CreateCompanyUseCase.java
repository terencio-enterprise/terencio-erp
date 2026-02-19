package es.terencio.erp.organization.application.port.in;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyResult;

public interface CreateCompanyUseCase {
    CreateCompanyResult execute(CreateCompanyCommand command);
}
