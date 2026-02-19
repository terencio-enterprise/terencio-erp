package es.terencio.erp.organization.application.port.in;

import java.util.UUID;

/**
 * Input port for deleting (soft-deleting) a store.
 * The store is deactivated if it has no active employees or non-inactive
 * devices.
 */
public interface DeleteStoreUseCase {
    void execute(UUID storeId);
}
