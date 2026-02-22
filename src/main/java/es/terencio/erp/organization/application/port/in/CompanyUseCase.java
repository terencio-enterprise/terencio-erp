package es.terencio.erp.organization.application.port.in;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyResult;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsResult;
import es.terencio.erp.organization.domain.model.Company;

public interface CompanyUseCase {
    CreateCompanyResult create(CreateCompanyCommand command);
    Company getById(UUID companyId);
    List<Company> getAllForEmployee(UUID employeeUuid);
    UpdateFiscalSettingsResult updateFiscalSettings(UUID companyId, UpdateFiscalSettingsCommand command);
}