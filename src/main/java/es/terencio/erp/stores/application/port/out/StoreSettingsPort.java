package es.terencio.erp.stores.application.port.out;

import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.stores.application.dto.StoreSettingsDto;

public interface StoreSettingsPort {
    Optional<StoreSettingsDto> findByStoreId(UUID storeId);

    void save(StoreSettingsDto settings);

    void update(StoreSettingsDto settings);
}
