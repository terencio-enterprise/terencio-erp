package es.terencio.erp.organization.application.port.in;

import java.util.UUID;

public interface DeleteStoreUseCase {
    void execute(UUID storeId);
}
