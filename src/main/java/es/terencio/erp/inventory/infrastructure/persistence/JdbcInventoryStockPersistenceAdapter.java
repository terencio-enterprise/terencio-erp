package es.terencio.erp.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.inventory.application.port.out.InventoryStockRepository;
import es.terencio.erp.inventory.domain.model.InventoryStock;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;
import es.terencio.erp.shared.domain.valueobject.Quantity;

/**
 * JDBC adapter for InventoryStock persistence.
 */
@Repository
public class JdbcInventoryStockPersistenceAdapter implements InventoryStockRepository {

    private final JdbcClient jdbcClient;

    public JdbcInventoryStockPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public InventoryStock save(InventoryStock stock) {
        jdbcClient.sql("""
                INSERT INTO inventory_stock (product_id, warehouse_id, company_id, quantity_on_hand,
                    last_updated_at, version)
                VALUES (:productId, :warehouseId, :companyId, :quantityOnHand, :lastUpdatedAt, :version)
                ON CONFLICT (product_id, warehouse_id) DO UPDATE SET
                    quantity_on_hand = EXCLUDED.quantity_on_hand,
                    last_updated_at = EXCLUDED.last_updated_at,
                    version = EXCLUDED.version
                """)
                .param("productId", stock.productId().value())
                .param("warehouseId", stock.warehouseId().value())
                .param("companyId", stock.companyId().value())
                .param("quantityOnHand", stock.quantityOnHand().value())
                .param("lastUpdatedAt", stock.lastUpdatedAt())
                .param("version", stock.version())
                .update();
        return stock;
    }

    @Override
    public Optional<InventoryStock> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId) {
        return jdbcClient.sql("""
                SELECT * FROM inventory_stock WHERE product_id = :productId AND warehouse_id = :warehouseId
                """)
                .param("productId", productId.value())
                .param("warehouseId", warehouseId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<InventoryStock> findByCompanyIdAndWarehouse(CompanyId companyId, WarehouseId warehouseId) {
        return jdbcClient.sql("""
                SELECT * FROM inventory_stock WHERE company_id = :companyId AND warehouse_id = :warehouseId
                ORDER BY product_id
                """)
                .param("companyId", companyId.value())
                .param("warehouseId", warehouseId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public List<InventoryStock> findByProductId(ProductId productId) {
        return jdbcClient.sql("""
                SELECT * FROM inventory_stock WHERE product_id = :productId
                """)
                .param("productId", productId.value())
                .query(this::mapRow)
                .list();
    }

    private InventoryStock mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new InventoryStock(
                new ProductId(rs.getLong("product_id")),
                new WarehouseId((UUID) rs.getObject("warehouse_id")),
                new CompanyId((UUID) rs.getObject("company_id")),
                Quantity.of((BigDecimal) rs.getObject("quantity_on_hand")),
                rs.getTimestamp("last_updated_at").toInstant(),
                rs.getLong("version"));
    }
}
