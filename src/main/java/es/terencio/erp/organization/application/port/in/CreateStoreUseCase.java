package es.terencio.erp.organization.application.port.in;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreResult;

public interface CreateStoreUseCase {
    CreateStoreResult execute(CreateStoreCommand command);
}
