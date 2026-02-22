package es.terencio.erp.organization.infrastructure.out.persistence;

import java.sql.Timestamp;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.organization.domain.model.FiscalRegime;
import es.terencio.erp.organization.domain.model.RoundingMode;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.TaxId;

@Repository
public class CompanyRepositoryAdapter implements CompanyRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public CompanyRepositoryAdapter(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<Company> companyRowMapper = (rs, rowNum) -> new Company(
        new CompanyId(rs.getObject("id", UUID.class)),
        rs.getString("name"),
        TaxId.of(rs.getString("tax_id")),
        Currency.getInstance(rs.getString("currency_code")),
        FiscalRegime.valueOf(rs.getString("fiscal_regime")),
        rs.getBoolean("price_includes_tax"),
        RoundingMode.valueOf(rs.getString("rounding_mode")),
        rs.getBoolean("is_active"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(),
        rs.getLong("version")
    );

    @Override
    public boolean existsByTaxId(String taxId) {
        String sql = "SELECT count(*) FROM companies WHERE tax_id = :taxId AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql, 
            new MapSqlParameterSource("taxId", taxId), 
            Integer.class);
        return count != null && count > 0;
    }

    @Override
    public boolean existsById(CompanyId companyId) {
        String sql = "SELECT count(*) FROM companies WHERE id = :companyId AND deleted_at IS NULL";
        Integer count = jdbcTemplate.queryForObject(sql, 
            new MapSqlParameterSource("companyId", companyId.value()), 
            Integer.class);
        return count != null && count > 0;
    }

    @Override
    public Company save(Company company) {
        if (existsById(company.id())) {
            String sql = "UPDATE companies SET name = :name, tax_id = :taxId, currency_code = :currencyCode, " +
                         "fiscal_regime = :fiscalRegime, price_includes_tax = :priceIncludesTax, " +
                         "rounding_mode = :roundingMode, is_active = :isActive, updated_at = :updatedAt, " +
                         "version = version + 1 WHERE id = :id";
            jdbcTemplate.update(sql, createParameterSource(company));
        } else {
            // NOTE: The Company domain model does not store organizationId. 
            // We use a subquery to fetch the root organization as a fallback to satisfy the NOT NULL constraint.
            String sql = "INSERT INTO companies (id, organization_id, name, slug, tax_id, currency_code, " +
                         "fiscal_regime, price_includes_tax, rounding_mode, is_active, created_at, updated_at, version) " +
                         "VALUES (:id, (SELECT id FROM organizations LIMIT 1), :name, :slug, :taxId, :currencyCode, " +
                         ":fiscalRegime, :priceIncludesTax, :roundingMode, :isActive, :createdAt, :updatedAt, :version)";
            jdbcTemplate.update(sql, createParameterSource(company));
        }
        return company;
    }

    @Override
    public Optional<Company> findById(CompanyId companyId) {
        String sql = "SELECT * FROM companies WHERE id = :companyId AND deleted_at IS NULL";
        try {
            Company company = jdbcTemplate.queryForObject(sql, 
                new MapSqlParameterSource("companyId", companyId.value()), 
                companyRowMapper);
            return Optional.ofNullable(company);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Company> findVisibleCompaniesByEmployeeUuid(UUID employeeUuid) {
        // Fetches companies belonging to the same organization as the requesting employee
        String sql = "SELECT c.* FROM companies c " +
                     "JOIN employees e ON c.organization_id = e.organization_id " +
                     "WHERE e.uuid = :employeeUuid AND c.deleted_at IS NULL " +
                     "ORDER BY c.created_at ASC";
                     
        return jdbcTemplate.query(sql, 
            new MapSqlParameterSource("employeeUuid", employeeUuid), 
            companyRowMapper);
    }

    private MapSqlParameterSource createParameterSource(Company company) {
        return new MapSqlParameterSource()
            .addValue("id", company.id().value())
            .addValue("name", company.name())
            .addValue("slug", generateSlug(company.name()))
            .addValue("taxId", company.taxId().value())
            .addValue("currencyCode", company.currencyCode())
            .addValue("fiscalRegime", company.fiscalRegime().name())
            .addValue("priceIncludesTax", company.priceIncludesTax())
            .addValue("roundingMode", company.roundingMode().name())
            .addValue("isActive", company.isActive())
            .addValue("createdAt", Timestamp.from(company.createdAt()))
            .addValue("updatedAt", Timestamp.from(company.updatedAt()))
            .addValue("version", company.version());
    }

    private String generateSlug(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                   .replaceAll("[^a-z0-9\\s-]", "")
                   .replaceAll("\\s+", "-");
    }
}