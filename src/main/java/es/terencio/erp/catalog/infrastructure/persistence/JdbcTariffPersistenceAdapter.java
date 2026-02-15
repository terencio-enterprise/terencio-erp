package es.terencio.erp.catalog.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.terencio.erp.catalog.application.port.out.TariffRepository;
import es.terencio.erp.catalog.domain.model.Tariff;
import es.terencio.erp.shared.domain.identifier.CompanyId;

/**
 * JDBC adapter for Tariff persistence.
 */
@Repository
public class JdbcTariffPersistenceAdapter implements TariffRepository {

    private final JdbcClient jdbcClient;

    public JdbcTariffPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Tariff save(Tariff tariff) {
        if (tariff.id() == null) {
            return insert(tariff);
        } else {
            return update(tariff);
        }
    }

    private Tariff insert(Tariff tariff) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql("""
                INSERT INTO tariffs (company_id, name, priority, price_type, is_default, active, version)
                VALUES (:companyId, :name, :priority, :priceType, :isDefault, :active, :version)
                RETURNING id
                """)
                .param("companyId", tariff.companyId().value())
                .param("name", tariff.name())
                .param("priority", tariff.priority())
                .param("priceType", tariff.priceType())
                .param("isDefault", tariff.isDefault())
                .param("active", tariff.isActive())
                .param("version", tariff.version())
                .update(keyHolder);

        Long generatedId = ((Number) keyHolder.getKeys().get("id")).longValue();

        return new Tariff(
                generatedId,
                tariff.companyId(),
                tariff.name(),
                tariff.priority(),
                tariff.priceType(),
                tariff.isDefault(),
                tariff.isActive(),
                tariff.version());
    }

    private Tariff update(Tariff tariff) {
        jdbcClient.sql("""
                UPDATE tariffs SET name = :name, priority = :priority, is_default = :isDefault,
                    active = :active, version = :version
                WHERE id = :id
                """)
                .param("id", tariff.id())
                .param("name", tariff.name())
                .param("priority", tariff.priority())
                .param("isDefault", tariff.isDefault())
                .param("active", tariff.isActive())
                .param("version", tariff.version())
                .update();

        return tariff;
    }

    @Override
    public Optional<Tariff> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM tariffs WHERE id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Tariff> findByCompanyId(CompanyId companyId) {
        return jdbcClient
                .sql("SELECT * FROM tariffs WHERE company_id = :companyId AND active = TRUE ORDER BY priority, name")
                .param("companyId", companyId.value())
                .query(this::mapRow)
                .list();
    }

    private Tariff mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Tariff(
                rs.getLong("id"),
                new CompanyId((UUID) rs.getObject("company_id")),
                rs.getString("name"),
                rs.getInt("priority"),
                rs.getString("price_type"),
                rs.getBoolean("is_default"),
                rs.getBoolean("active"),
                rs.getLong("version"));
    }
}
