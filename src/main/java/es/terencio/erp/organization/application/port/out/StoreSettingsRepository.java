package es.terencio.erp.organization.application.port.out;

import java.util.Optional;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.domain.identifier.StoreId;

public interface StoreSettingsRepository {
    StoreSettings save(StoreSettings settings);
    Optional<StoreSettings> findByStoreId(StoreId storeId);
}
