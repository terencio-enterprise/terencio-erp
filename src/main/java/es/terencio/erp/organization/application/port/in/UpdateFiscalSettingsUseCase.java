package es.terencio.erp.organization.application.port.in;

import java.util.UUID;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsResult;

public interface UpdateFiscalSettingsUseCase {
    UpdateFiscalSettingsResult execute(UUID companyId, UpdateFiscalSettingsCommand command);
}
