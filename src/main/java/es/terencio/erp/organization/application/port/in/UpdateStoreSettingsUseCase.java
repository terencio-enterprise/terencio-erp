package es.terencio.erp.organization.application.port.in;

import es.terencio.erp.organization.application.usecase.UpdateStoreSettingsCommand;

/**
 * Input port for updating store settings.
 */
public interface UpdateStoreSettingsUseCase {
    void execute(UpdateStoreSettingsCommand command);
}
