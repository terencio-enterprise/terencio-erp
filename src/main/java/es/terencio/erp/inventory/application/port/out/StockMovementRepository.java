package es.terencio.erp.inventory.application.port.out;

import java.util.List;

import es.terencio.erp.inventory.domain.model.StockMovement;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;

/**
 * Output port for StockMovement persistence.
 */
public interface StockMovementRepository {

    StockMovement save(StockMovement movement);

    List<StockMovement> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId);

    List<StockMovement> findByProduct(ProductId productId);
}
