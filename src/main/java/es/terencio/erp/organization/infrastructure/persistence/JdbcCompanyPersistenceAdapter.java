package es.terencio.erp.organization.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.organization.domain.model.FiscalRegime;
import es.terencio.erp.organization.domain.model.RoundingMode;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.TaxId;

/**
 * JDBC adapter for Company persistence.
 */
@Repository("organizationCompanyAdapter")
public class JdbcCompanyPersistenceAdapter implements CompanyRepository {

    private final JdbcClient jdbcClient;

    public JdbcCompanyPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Company save(Company company) {
        UUID id = company.id() != null ? company.id().value() : UUID.randomUUID();

        jdbcClient.sql("""
                INSERT INTO companies (id, name, tax_id, currency_code, fiscal_regime,
                    price_includes_tax, rounding_mode, is_active, created_at, updated_at, version)
                VALUES (:id, :name, :taxId, :currency, :fiscalRegime, :priceIncludesTax,
                    :roundingMode, :isActive, :createdAt, :updatedAt, :version)
                ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    tax_id = EXCLUDED.tax_id,
                    currency_code = EXCLUDED.currency_code,
                    fiscal_regime = EXCLUDED.fiscal_regime,
                    price_includes_tax = EXCLUDED.price_includes_tax,
                    rounding_mode = EXCLUDED.rounding_mode,
                    is_active = EXCLUDED.is_active,
                    updated_at = EXCLUDED.updated_at,
                    version = EXCLUDED.version
                """)
                .param("id", id)
                .param("name", company.name())
                .param("taxId", company.taxId().value())
                .param("currency", company.currencyCode())
                .param("fiscalRegime", company.fiscalRegime().name())
                .param("priceIncludesTax", company.priceIncludesTax())
                .param("roundingMode", company.roundingMode().name())
                .param("isActive", company.isActive())
                .param("createdAt", Timestamp.from(company.createdAt()))
                .param("updatedAt", Timestamp.from(company.updatedAt()))
                .param("version", company.version())
                .update();

        return new Company(
                new CompanyId(id),
                company.name(),
                company.taxId(),
                company.currency(),
                company.fiscalRegime(),
                company.priceIncludesTax(),
                company.roundingMode(),
                company.isActive(),
                company.createdAt(),
                company.updatedAt(),
                company.version());
    }

    @Override
    public Optional<Company> findById(CompanyId id) {
        return jdbcClient.sql("""
                SELECT * FROM companies WHERE id = :id
                """)
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public boolean existsByTaxId(String taxId) {
        Integer count = jdbcClient.sql("SELECT COUNT(*) FROM companies WHERE tax_id = :taxId")
                .param("taxId", taxId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    private Company mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Company(
                new CompanyId((UUID) rs.getObject("id")),
                rs.getString("name"),
                TaxId.of(rs.getString("tax_id")),
                Currency.getInstance(rs.getString("currency_code")),
                FiscalRegime.valueOf(rs.getString("fiscal_regime")),
                rs.getBoolean("price_includes_tax"),
                RoundingMode.valueOf(rs.getString("rounding_mode")),
                rs.getBoolean("is_active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"));
    }
}
