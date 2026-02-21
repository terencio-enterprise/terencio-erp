package es.terencio.erp.crm.infrastructure.out.persistence;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.crm.application.port.out.CustomerRepositoryPort;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;
import es.terencio.erp.crm.domain.model.MarketingStatus;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.TaxId;

@Repository
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {

    private final JdbcClient jdbcClient;

    public CustomerPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public Customer save(Customer customer) {
        String sql = """
                INSERT INTO customers (
                    uuid, company_id, tax_id, legal_name, commercial_name, email, phone,
                    address, zip_code, city, country, tariff_id, allow_credit, credit_limit,
                    surcharge_apply, notes, active, type, origin, tags, marketing_consent,
                    marketing_status, unsubscribe_token, last_interaction_at, created_at, updated_at
                ) VALUES (
                    :uuid, :companyId, :taxId, :legalName, :commercialName, :email, :phone,
                    :address, :zipCode, :city, :country, :tariffId, :allowCredit, :creditLimit,
                    :surchargeApply, :notes, :active, :type, :origin, :tags, :marketingConsent,
                    :marketingStatus, :unsubscribeToken, :lastInteractionAt, :createdAt, :updatedAt
                )
                ON CONFLICT (uuid) DO UPDATE SET
                    legal_name = EXCLUDED.legal_name,
                    commercial_name = EXCLUDED.commercial_name,
                    email = EXCLUDED.email,
                    phone = EXCLUDED.phone,
                    address = EXCLUDED.address,
                    zip_code = EXCLUDED.zip_code,
                    city = EXCLUDED.city,
                    country = EXCLUDED.country,
                    tariff_id = EXCLUDED.tariff_id,
                    allow_credit = EXCLUDED.allow_credit,
                    credit_limit = EXCLUDED.credit_limit,
                    surcharge_apply = EXCLUDED.surcharge_apply,
                    notes = EXCLUDED.notes,
                    active = EXCLUDED.active,
                    type = EXCLUDED.type,
                    origin = EXCLUDED.origin,
                    tags = EXCLUDED.tags,
                    marketing_consent = EXCLUDED.marketing_consent,
                    marketing_status = EXCLUDED.marketing_status,
                    last_interaction_at = EXCLUDED.last_interaction_at,
                    updated_at = EXCLUDED.updated_at
                """;

        jdbcClient.sql(sql)
                .param("uuid", customer.uuid())
                .param("companyId", customer.companyId().value())
                .param("taxId", customer.taxId() != null ? customer.taxId().value() : null)
                .param("legalName", customer.legalName())
                .param("commercialName", customer.commercialName())
                .param("email", customer.email() != null ? customer.email().value() : null)
                .param("phone", customer.phone())
                .param("address", customer.address())
                .param("zipCode", customer.zipCode())
                .param("city", customer.city())
                .param("country", customer.country())
                .param("tariffId", customer.tariffId())
                .param("allowCredit", customer.allowCredit())
                .param("creditLimit", customer.creditLimit() != null ? customer.creditLimit().cents() : 0)
                .param("surchargeApply", customer.surchargeApply())
                .param("notes", customer.notes())
                .param("active", customer.isActive())
                .param("type", customer.getType() != null ? customer.getType().name() : "CLIENT")
                .param("origin", customer.getOrigin())
                .param("tags", customer.getTags() != null ? customer.getTags().toArray(new String[0]) : new String[0])
                .param("marketingConsent", customer.isMarketingConsent())
                .param("marketingStatus",
                        customer.getMarketingStatus() != null ? customer.getMarketingStatus().name() : "SUBSCRIBED")
                .param("unsubscribeToken", customer.getUnsubscribeToken())
                .param("lastInteractionAt",
                        customer.getLastInteractionAt() != null
                                ? java.sql.Timestamp.from(customer.getLastInteractionAt())
                                : null)
                .param("createdAt", java.sql.Timestamp.from(customer.createdAt()))
                .param("updatedAt", java.sql.Timestamp.from(customer.updatedAt()))
                .update();

        return customer;
    }

    @Override
    public Optional<Customer> findByUuid(UUID uuid) {
        return jdbcClient.sql("SELECT * FROM customers WHERE uuid = ?")
                .param(uuid)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Customer> findByCompanyId(CompanyId companyId) {
        return jdbcClient.sql("SELECT * FROM customers WHERE company_id = ? AND deleted_at IS NULL")
                .param(companyId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public List<Customer> searchCustomers(CompanyId companyId, String search) {
        // Uses ILIKE which is now performant thanks to the Gin Trigram index in Canvas
        String searchParam = "%" + search + "%";
        return jdbcClient.sql("""
                SELECT * FROM customers
                WHERE company_id = ?
                AND deleted_at IS NULL
                AND (legal_name ILIKE ? OR email ILIKE ? OR tax_id ILIKE ?)
                """)
                .param(companyId.value())
                .param(searchParam)
                .param(searchParam)
                .param(searchParam)
                .query(this::mapRow)
                .list();
    }

    private Customer mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        String emailStr = rs.getString("email");
        String taxIdStr = rs.getString("tax_id");

        Customer c = new Customer(
                new CustomerId(rs.getLong("id")),
                UUID.fromString(rs.getString("uuid")),
                new CompanyId(UUID.fromString(rs.getString("company_id"))),
                taxIdStr != null ? TaxId.of(taxIdStr) : null,
                rs.getString("legal_name"),
                rs.getString("commercial_name"),
                emailStr != null ? Email.of(emailStr) : null,
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("zip_code"),
                rs.getString("city"),
                rs.getString("country"),
                rs.getObject("tariff_id", Long.class),
                rs.getBoolean("allow_credit"),
                Money.ofEurosCents(rs.getLong("credit_limit")),
                rs.getBoolean("surcharge_apply"),
                rs.getString("notes"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getTimestamp("deleted_at") != null ? rs.getTimestamp("deleted_at").toInstant() : null);

        // Map CRM/Marketing specific fields
        String typeStr = rs.getString("type");
        if (typeStr != null)
            c.setType(CustomerType.valueOf(typeStr));

        String statusStr = rs.getString("marketing_status");
        if (statusStr != null)
            c.setMarketingStatus(MarketingStatus.valueOf(statusStr));

        c.setOrigin(rs.getString("origin"));
        c.setMarketingConsent(rs.getBoolean("marketing_consent"));
        c.setUnsubscribeToken(rs.getString("unsubscribe_token"));

        if (rs.getTimestamp("last_interaction_at") != null) {
            c.setLastInteractionAt(rs.getTimestamp("last_interaction_at").toInstant());
        }

        // Map Postgres TEXT[] to Java List<String>
        java.sql.Array tagsArray = rs.getArray("tags");
        if (tagsArray != null) {
            String[] tags = (String[]) tagsArray.getArray();
            c.setTags(Arrays.asList(tags));
        }

        return c;
    }
}