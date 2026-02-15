package es.terencio.erp.organization.application.port.out;

import es.terencio.erp.organization.domain.model.Warehouse;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;

import java.util.Optional;

/**
 * Output port for Warehouse persistence.
 */
public interface WarehouseRepository {

    Warehouse save(Warehouse warehouse);

    Optional<Warehouse> findById(WarehouseId id);

    Optional<Warehouse> findByStoreId(StoreId storeId);

    boolean existsByStoreId(StoreId storeId);
}
