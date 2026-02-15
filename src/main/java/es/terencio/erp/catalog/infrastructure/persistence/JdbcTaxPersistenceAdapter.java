package es.terencio.erp.catalog.infrastructure.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.terencio.erp.catalog.application.port.out.TaxRepository;
import es.terencio.erp.catalog.domain.model.Tax;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.TaxRate;

/**
 * JDBC adapter for Tax persistence.
 */
@Repository
public class JdbcTaxPersistenceAdapter implements TaxRepository {

    private final JdbcClient jdbcClient;

    public JdbcTaxPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Tax save(Tax tax) {
        if (tax.id() == null) {
            return insert(tax);
        } else {
            return update(tax);
        }
    }

    private Tax insert(Tax tax) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql("""
                INSERT INTO taxes (company_id, name, rate, surcharge, code_aeat, active, created_at)
                VALUES (:companyId, :name, :rate, :surcharge, :codeAeat, :active, :createdAt)
                RETURNING id
                """)
                .param("companyId", tax.companyId().value())
                .param("name", tax.name())
                .param("rate", tax.rate().rate())
                .param("surcharge", tax.surcharge().rate())
                .param("codeAeat", tax.codeAeat())
                .param("active", tax.isActive())
                .param("createdAt", tax.createdAt())
                .update(keyHolder);

        Long generatedId = ((Number) keyHolder.getKeys().get("id")).longValue();

        return new Tax(
                generatedId,
                tax.companyId(),
                tax.name(),
                tax.rate(),
                tax.surcharge(),
                tax.codeAeat(),
                tax.isActive(),
                tax.createdAt());
    }

    private Tax update(Tax tax) {
        jdbcClient.sql("""
                UPDATE taxes SET name = :name, rate = :rate, surcharge = :surcharge,
                    code_aeat = :codeAeat, active = :active
                WHERE id = :id
                """)
                .param("id", tax.id())
                .param("name", tax.name())
                .param("rate", tax.rate().rate())
                .param("surcharge", tax.surcharge().rate())
                .param("codeAeat", tax.codeAeat())
                .param("active", tax.isActive())
                .update();

        return tax;
    }

    @Override
    public Optional<Tax> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM taxes WHERE id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Tax> findByCompanyId(CompanyId companyId) {
        return jdbcClient.sql("SELECT * FROM taxes WHERE company_id = :companyId AND active = TRUE ORDER BY name")
                .param("companyId", companyId.value())
                .query(this::mapRow)
                .list();
    }

    private Tax mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Tax(
                rs.getLong("id"),
                new CompanyId((UUID) rs.getObject("company_id")),
                rs.getString("name"),
                TaxRate.of((BigDecimal) rs.getObject("rate")),
                TaxRate.of((BigDecimal) rs.getObject("surcharge")),
                rs.getString("code_aeat"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant());
    }
}
