package es.terencio.erp.organization.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.domain.model.Address;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;

/**
 * JDBC adapter for Store persistence.
 */
@Repository("organizationStoreAdapter")
public class JdbcStorePersistenceAdapter implements StoreRepository {

    private final JdbcClient jdbcClient;

    public JdbcStorePersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Store save(Store store) {
        UUID id = store.id() != null ? store.id().value() : UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO stores (id, company_id, code, name, address, zip_code, city,
                    is_active, timezone, created_at, updated_at, version)
                VALUES (:id, :companyId, :code, :name, :address, :zipCode, :city,
                    :isActive, :timezone, :createdAt, :updatedAt, :version)
                ON CONFLICT (id) DO UPDATE SET
                    code = EXCLUDED.code,
                    name = EXCLUDED.name,
                    address = EXCLUDED.address,
                    zip_code = EXCLUDED.zip_code,
                    city = EXCLUDED.city,
                    is_active = EXCLUDED.is_active,
                    timezone = EXCLUDED.timezone,
                    updated_at = EXCLUDED.updated_at,
                    version = EXCLUDED.version
                """)
                .param("id", id)
                .param("companyId", store.companyId().value())
                .param("code", store.code())
                .param("name", store.name())
                .param("address", store.address() != null ? store.address().street() : null)
                .param("zipCode", store.address() != null ? store.address().zipCode() : null)
                .param("city", store.address() != null ? store.address().city() : null)
                .param("isActive", store.isActive())
                .param("timezone", store.timezone().getId())
                .param("createdAt", store.createdAt())
                .param("updatedAt", store.updatedAt())
                .param("version", store.version())
                .update();

        return new Store(
                new StoreId(id),
                store.companyId(),
                store.code(),
                store.name(),
                store.address(),
                store.taxId(),
                store.timezone(),
                store.isActive(),
                store.createdAt(),
                store.updatedAt(),
                store.version());
    }

    @Override
    public Optional<Store> findById(StoreId id) {
        return jdbcClient.sql("""
                SELECT * FROM stores WHERE id = :id
                """)
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Store> findByCompanyId(CompanyId companyId) {
        return jdbcClient.sql("""
                SELECT * FROM stores WHERE company_id = :companyId ORDER BY name
                """)
                .param("companyId", companyId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public boolean existsByCompanyAndCode(CompanyId companyId, String code) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*) FROM stores WHERE company_id = :companyId AND code = :code
                """)
                .param("companyId", companyId.value())
                .param("code", code)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    private Store mapRow(ResultSet rs, int rowNum) throws SQLException {
        String street = rs.getString("address");
        String zipCode = rs.getString("zip_code");
        String city = rs.getString("city");
        Address address = (street != null || zipCode != null || city != null)
                ? new Address(street, zipCode, city, "ES")
                : null;

        return new Store(
                new StoreId((UUID) rs.getObject("id")),
                new CompanyId((UUID) rs.getObject("company_id")),
                rs.getString("code"),
                rs.getString("name"),
                address,
                null, // taxId not stored in this version
                ZoneId.of(rs.getString("timezone")),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"));
    }
}
