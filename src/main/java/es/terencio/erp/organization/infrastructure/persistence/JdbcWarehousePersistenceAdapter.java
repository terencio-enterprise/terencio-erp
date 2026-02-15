package es.terencio.erp.organization.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.port.out.WarehouseRepository;
import es.terencio.erp.organization.domain.model.Warehouse;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.identifier.WarehouseId;

/**
 * JDBC adapter for Warehouse persistence.
 */
@Repository("organizationWarehouseAdapter")
public class JdbcWarehousePersistenceAdapter implements WarehouseRepository {

    private final JdbcClient jdbcClient;

    public JdbcWarehousePersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Warehouse save(Warehouse warehouse) {
        UUID id = warehouse.id() != null ? warehouse.id().value() : UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO warehouses (id, store_id, name, code, created_at)
                VALUES (:id, :storeId, :name, :code, :createdAt)
                ON CONFLICT (store_id) DO UPDATE SET
                    name = EXCLUDED.name,
                    code = EXCLUDED.code
                """)
                .param("id", id)
                .param("storeId", warehouse.storeId().value())
                .param("name", warehouse.name())
                .param("code", warehouse.code())
                .param("createdAt", Timestamp.from(warehouse.createdAt()))
                .update();

        return new Warehouse(
                new WarehouseId(id),
                warehouse.storeId(),
                warehouse.name(),
                warehouse.code(),
                warehouse.createdAt());
    }

    @Override
    public Optional<Warehouse> findByStoreId(StoreId storeId) {
        return jdbcClient.sql("""
                SELECT * FROM warehouses WHERE store_id = :storeId
                """)
                .param("storeId", storeId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<Warehouse> findById(WarehouseId id) {
        return jdbcClient.sql("""
                SELECT * FROM warehouses WHERE id = :id
                """)
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public boolean existsByStoreId(StoreId storeId) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*) FROM warehouses WHERE store_id = :storeId
                """)
                .param("storeId", storeId.value())
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    private Warehouse mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Warehouse(
                new WarehouseId((UUID) rs.getObject("id")),
                new StoreId((UUID) rs.getObject("store_id")),
                rs.getString("name"),
                rs.getString("code"),
                rs.getTimestamp("created_at").toInstant());
    }
}
