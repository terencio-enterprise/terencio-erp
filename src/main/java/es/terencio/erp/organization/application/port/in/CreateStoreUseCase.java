package es.terencio.erp.organization.application.port.in;

import es.terencio.erp.organization.application.usecase.CreateStoreCommand;
import es.terencio.erp.organization.application.usecase.CreateStoreResult;

/**
 * Input port for creating stores.
 * When a store is created, a default warehouse is auto-created.
 */
public interface CreateStoreUseCase {
    CreateStoreResult execute(CreateStoreCommand command);
}
