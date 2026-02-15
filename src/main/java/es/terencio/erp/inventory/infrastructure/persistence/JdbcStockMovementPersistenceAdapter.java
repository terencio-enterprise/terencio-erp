package es.terencio.erp.inventory.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.terencio.erp.inventory.application.port.out.StockMovementRepository;
import es.terencio.erp.inventory.domain.model.StockMovement;
import es.terencio.erp.inventory.domain.model.StockMovementType;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.identifier.SaleId;
import es.terencio.erp.shared.domain.identifier.UserId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.Quantity;

/**
 * JDBC adapter for StockMovement persistence.
 */
@Repository
public class JdbcStockMovementPersistenceAdapter implements StockMovementRepository {

    private final JdbcClient jdbcClient;

    public JdbcStockMovementPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public StockMovement save(StockMovement movement) {
        UUID uuid = movement.uuid() != null ? movement.uuid() : UUID.randomUUID();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql("""
                INSERT INTO stock_movements (uuid, product_id, warehouse_id, type, quantity,
                    previous_balance, new_balance, cost_unit, reason, reference_doc_type,
                    reference_doc_uuid, user_id, created_at)
                VALUES (:uuid, :productId, :warehouseId, :type, :quantity,
                    :previousBalance, :newBalance, :costUnit, :reason, :referenceDocType,
                    :referenceDocUuid, :userId, :createdAt)
                RETURNING id
                """)
                .param("uuid", uuid)
                .param("productId", movement.productId().value())
                .param("warehouseId", movement.warehouseId().value())
                .param("type", movement.type().name())
                .param("quantity", movement.quantity().value())
                .param("previousBalance", movement.previousBalance().value())
                .param("newBalance", movement.newBalance().value())
                .param("costUnit", movement.costUnit() != null ? movement.costUnit().cents() : null)
                .param("reason", movement.reason())
                .param("referenceDocType", movement.referenceDocType())
                .param("referenceDocUuid",
                        movement.referenceDocUuid() != null ? movement.referenceDocUuid().value() : null)
                .param("userId", movement.userId() != null ? movement.userId().value() : null)
                .param("createdAt", movement.createdAt())
                .update(keyHolder);

        Long generatedId = ((Number) keyHolder.getKeys().get("id")).longValue();

        return new StockMovement(
                generatedId,
                uuid,
                movement.productId(),
                movement.warehouseId(),
                movement.type(),
                movement.quantity(),
                movement.previousBalance(),
                movement.newBalance(),
                movement.costUnit(),
                movement.reason(),
                movement.referenceDocType(),
                movement.referenceDocUuid(),
                movement.userId(),
                movement.createdAt());
    }

    @Override
    public List<StockMovement> findByProductAndWarehouse(ProductId productId, WarehouseId warehouseId) {
        return jdbcClient.sql("""
                SELECT * FROM stock_movements
                WHERE product_id = :productId AND warehouse_id = :warehouseId
                ORDER BY created_at DESC
                """)
                .param("productId", productId.value())
                .param("warehouseId", warehouseId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public List<StockMovement> findByProduct(ProductId productId) {
        return jdbcClient.sql("""
                SELECT * FROM stock_movements
                WHERE product_id = :productId
                ORDER BY created_at DESC
                LIMIT 100
                """)
                .param("productId", productId.value())
                .query(this::mapRow)
                .list();
    }

    private StockMovement mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long costUnitCents = (Long) rs.getObject("cost_unit");
        Long userId = (Long) rs.getObject("user_id");
        UUID referenceDocUuid = (UUID) rs.getObject("reference_doc_uuid");

        return new StockMovement(
                rs.getLong("id"),
                (UUID) rs.getObject("uuid"),
                new ProductId(rs.getLong("product_id")),
                new WarehouseId((UUID) rs.getObject("warehouse_id")),
                StockMovementType.valueOf(rs.getString("type")),
                Quantity.of((BigDecimal) rs.getObject("quantity")),
                Quantity.of((BigDecimal) rs.getObject("previous_balance")),
                Quantity.of((BigDecimal) rs.getObject("new_balance")),
                costUnitCents != null ? Money.ofEurosCents(costUnitCents) : null,
                rs.getString("reason"),
                rs.getString("reference_doc_type"),
                referenceDocUuid != null ? new SaleId(referenceDocUuid) : null,
                userId != null ? new UserId(userId) : null,
                rs.getTimestamp("created_at").toInstant());
    }
}
