package es.terencio.erp.inventory.application.port.out;

import es.terencio.erp.inventory.domain.model.InventoryStock;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;

import java.util.Optional;

/**
 * Output port for InventoryStock persistence.
 */
public interface InventoryStockRepository {

    InventoryStock save(InventoryStock stock);

    Optional<InventoryStock> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId);
}
