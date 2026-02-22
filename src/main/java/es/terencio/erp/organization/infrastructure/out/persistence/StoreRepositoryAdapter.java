package es.terencio.erp.organization.infrastructure.out.persistence;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.domain.model.Address;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;

@Repository
public class StoreRepositoryAdapter implements StoreRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StoreRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Store> storeRowMapper = (rs, rowNum) -> new Store(
        new StoreId(rs.getObject("id", UUID.class)),
        new CompanyId(rs.getObject("company_id", UUID.class)),
        rs.getString("code"),
        rs.getString("name"),
        new Address(rs.getString("address"), rs.getString("zip_code"), rs.getString("city"), "ES"),
        null, // Note: tax_id is present in the Domain model but missing in the V001_core_schema.sql 'stores' table
        ZoneId.of(rs.getString("timezone")),
        rs.getBoolean("is_active"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(),
        rs.getLong("version")
    );

    @Override
    public boolean existsByCompanyAndCode(CompanyId companyId, String code) {
        String sql = "SELECT count(*) FROM stores WHERE company_id = :companyId AND code = :code AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql,
            new MapSqlParameterSource()
                .addValue("companyId", companyId.value())
                .addValue("code", code),
            Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsById(StoreId storeId) {
        String sql = "SELECT count(*) FROM stores WHERE id = :storeId AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql,
            new MapSqlParameterSource("storeId", storeId.value()),
            Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean hasDependencies(StoreId storeId) {
        String sql = "SELECT CASE WHEN " +
                     "EXISTS (SELECT 1 FROM devices WHERE store_id = :storeId) OR " +
                     "EXISTS (SELECT 1 FROM employees WHERE last_active_store_id = :storeId) " +
                     "THEN TRUE ELSE FALSE END";
        Boolean hasDeps = jdbcTemplate.queryForObject(sql,
            new MapSqlParameterSource("storeId", storeId.value()),
            Boolean.class);
        return Boolean.TRUE.equals(hasDeps);
    }

    @Override
    public Store save(Store store) {
        if (existsById(store.id())) {
            String sql = "UPDATE stores SET name = :name, address = :address, zip_code = :zipCode, " +
                         "city = :city, is_active = :isActive, timezone = :timezone, " +
                         "updated_at = :updatedAt, version = version + 1 " +
                         "WHERE id = :id";
            jdbcTemplate.update(sql, createParameterSource(store));
        } else {
            // Include dynamic slug generation for new stores as required by the schema
            String sql = "INSERT INTO stores (id, company_id, code, name, slug, address, zip_code, city, is_active, timezone, created_at, updated_at, version) " +
                         "VALUES (:id, :companyId, :code, :name, :slug, :address, :zipCode, :city, :isActive, :timezone, :createdAt, :updatedAt, :version)";
            jdbcTemplate.update(sql, createParameterSource(store));
        }
        return store;
    }

    @Override
    public Optional<Store> findById(StoreId storeId) {
        String sql = "SELECT * FROM stores WHERE id = :storeId AND deleted_at IS NULL";
        try {
            Store store = jdbcTemplate.queryForObject(sql,
                new MapSqlParameterSource("storeId", storeId.value()),
                storeRowMapper);
            return Optional.ofNullable(store);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Store> findByCompanyId(CompanyId companyId) {
        String sql = "SELECT * FROM stores WHERE company_id = :companyId AND deleted_at IS NULL ORDER BY created_at ASC";
        return jdbcTemplate.query(sql,
            new MapSqlParameterSource("companyId", companyId.value()),
            storeRowMapper);
    }

    @Override
    public void delete(StoreId storeId) {
        // Soft delete implementation as specified by the deleted_at column in schema
        String sql = "UPDATE stores SET deleted_at = NOW(), is_active = FALSE WHERE id = :storeId";
        jdbcTemplate.update(sql, new MapSqlParameterSource("storeId", storeId.value()));
    }

    private MapSqlParameterSource createParameterSource(Store store) {
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", store.id().value())
            .addValue("companyId", store.companyId().value())
            .addValue("code", store.code())
            .addValue("name", store.name())
            .addValue("slug", generateSlug(store.name()))
            .addValue("isActive", store.isActive())
            .addValue("timezone", store.timezone().getId())
            .addValue("createdAt", Timestamp.from(store.createdAt()))
            .addValue("updatedAt", Timestamp.from(store.updatedAt()))
            .addValue("version", store.version());

        if (store.address() != null) {
            params.addValue("address", store.address().street())
                  .addValue("zipCode", store.address().zipCode())
                  .addValue("city", store.address().city());
        } else {
            params.addValue("address", null)
                  .addValue("zipCode", null)
                  .addValue("city", null);
        }

        return params;
    }

    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-");
    }
}