package es.terencio.erp.organization.application.port.in;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreResult;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateStoreSettingsCommand;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;

public interface StoreUseCase {
    CreateStoreResult create(CreateStoreCommand command);
    Store getById(UUID storeId);
    List<Store> getAllByCompany(UUID companyId);
    StoreSettings getSettings(UUID storeId);
    void updateSettings(UpdateStoreSettingsCommand command);
    void delete(UUID storeId);
}