package es.terencio.erp.stores.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.stores.application.dto.StoreDto;
import es.terencio.erp.stores.application.port.out.StorePort;

@Repository
public class JdbcStoreAdapter implements StorePort {

    private final JdbcClient jdbcClient;

    public JdbcStoreAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public List<StoreDto> findAll() {
        return jdbcClient.sql("SELECT id, code, name, address, tax_id, is_active FROM stores ORDER BY name")
                .query(StoreDto.class)
                .list();
    }

    @Override
    public Optional<StoreDto> findById(UUID id) {
        return jdbcClient.sql("SELECT id, code, name, address, tax_id, is_active FROM stores WHERE id = :id")
                .param("id", id)
                .query(StoreDto.class)
                .optional();
    }

    @Override
    public Optional<StoreDto> findByCode(String code) {
        return jdbcClient.sql("SELECT id, code, name, address, tax_id, is_active FROM stores WHERE code = :code")
                .param("code", code)
                .query(StoreDto.class)
                .optional();
    }

    @Override
    public void save(StoreDto store) {
        jdbcClient.sql("""
                INSERT INTO stores (id, code, name, address, tax_id, is_active, created_at, updated_at)
                VALUES (:id, :code, :name, :address, :taxId, :isActive, NOW(), NOW())
                """)
                .param("id", store.id())
                .param("code", store.code())
                .param("name", store.name())
                .param("address", store.address())
                .param("taxId", store.taxId())
                .param("isActive", store.isActive())
                .update();
    }

    @Override
    public void update(StoreDto store) {
        jdbcClient.sql("""
                UPDATE stores 
                SET code = :code, name = :name, address = :address, tax_id = :taxId, 
                    is_active = :isActive, updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", store.id())
                .param("code", store.code())
                .param("name", store.name())
                .param("address", store.address())
                .param("taxId", store.taxId())
                .param("isActive", store.isActive())
                .update();
    }

    @Override
    public boolean hasDependencies(UUID id) {
        Integer userCount = jdbcClient.sql("SELECT COUNT(*) FROM users WHERE store_id = :id AND is_active = TRUE")
                .param("id", id)
                .query(Integer.class)
                .single();

        Integer deviceCount = jdbcClient.sql("SELECT COUNT(*) FROM devices WHERE store_id = :id AND status != 'INACTIVE'")
                .param("id", id)
                .query(Integer.class)
                .single();

        return (userCount != null && userCount > 0) || (deviceCount != null && deviceCount > 0);
    }
}