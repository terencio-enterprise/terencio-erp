package es.terencio.erp.organization.application.port.out;

import java.util.Optional;
import es.terencio.erp.organization.domain.model.Warehouse;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;

public interface WarehouseRepository {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findByStoreId(StoreId storeId);
    Optional<Warehouse> findById(WarehouseId id);
    boolean existsByStoreId(StoreId storeId);
}
