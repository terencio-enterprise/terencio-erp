package es.terencio.erp.organization.application.port.in;

import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateStoreSettingsCommand;

public interface UpdateStoreSettingsUseCase {
    void execute(UpdateStoreSettingsCommand command);
}
