package es.terencio.erp.organization.application.port.out;

import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.domain.identifier.StoreId;

import java.util.Optional;

/**
 * Output port for StoreSettings persistence.
 */
public interface StoreSettingsRepository {

    StoreSettings save(StoreSettings settings);

    Optional<StoreSettings> findByStoreId(StoreId storeId);
}
