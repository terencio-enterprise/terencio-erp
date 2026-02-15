package es.terencio.erp.inventory.application.port.out;

import java.util.List;
import java.util.Optional;

import es.terencio.erp.inventory.domain.model.InventoryStock;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;

/**
 * Output port for InventoryStock persistence.
 */
public interface InventoryStockRepository {

    InventoryStock save(InventoryStock stock);

    Optional<InventoryStock> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId);

    List<InventoryStock> findByProductId(ProductId productId);

    List<InventoryStock> findByCompanyIdAndWarehouse(CompanyId companyId, WarehouseId warehouseId);
}
