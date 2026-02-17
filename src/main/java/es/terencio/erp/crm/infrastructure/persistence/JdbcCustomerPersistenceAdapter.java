package es.terencio.erp.crm.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.terencio.erp.crm.application.port.out.CustomerRepository;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.TaxId;

/**
 * JDBC adapter for Customer persistence.
 */
@Repository
public class JdbcCustomerPersistenceAdapter implements CustomerRepository {

    private final JdbcClient jdbcClient;

    public JdbcCustomerPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Customer save(Customer customer) {
        if (customer.id() == null) {
            return insert(customer);
        } else {
            return update(customer);
        }
    }

    private Customer insert(Customer customer) {
        UUID uuid = customer.uuid() != null ? customer.uuid() : UUID.randomUUID();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient
                .sql("""
                        INSERT INTO customers (uuid, company_id, tax_id, legal_name, commercial_name,
                            email, phone, address, zip_code, city, country, tariff_id, allow_credit,
                            credit_limit, surcharge_apply, notes, active, created_at, updated_at,
                            type, origin, tags, marketing_consent, marketing_status, unsubscribe_token, last_interaction_at, snoozed_until)
                        VALUES (:uuid, :companyId, :taxId, :legalName, :commercialName, :email, :phone,
                            :address, :zipCode, :city, :country, :tariffId, :allowCredit, :creditLimit,
                            :surchargeApply, :notes, :active, :createdAt, :updatedAt,
                            :type, :origin, :tags, :marketingConsent, :marketingStatus, :unsubscribeToken, :lastInteractionAt, :snoozedUntil)
                        RETURNING id
                        """)
                .param("uuid", uuid)
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
                .param("creditLimit", customer.creditLimit().cents())
                .param("surchargeApply", customer.surchargeApply())
                .param("notes", customer.notes())
                .param("active", customer.isActive())
                .param("createdAt", Timestamp.from(customer.createdAt()))
                .param("updatedAt", Timestamp.from(customer.updatedAt()))
                .param("type", customer.getType())
                .param("origin", customer.getOrigin())
                .param("tags", customer.getTags())
                .param("marketingConsent", customer.isMarketingConsent())
                .param("marketingStatus", customer.getMarketingStatus())
                .param("unsubscribeToken", customer.getUnsubscribeToken())
                .param("lastInteractionAt",
                        customer.getLastInteractionAt() != null ? Timestamp.from(customer.getLastInteractionAt())
                                : null)
                .param("snoozedUntil",
                        customer.getSnoozedUntil() != null ? Timestamp.from(customer.getSnoozedUntil()) : null)
                .update(keyHolder);

        Long generatedId = ((Number) keyHolder.getKeys().get("id")).longValue();

        Customer newCustomer = new Customer(
                new CustomerId(generatedId),
                uuid,
                customer.companyId(),
                customer.taxId(),
                customer.legalName(),
                customer.commercialName(),
                customer.email(),
                customer.phone(),
                customer.address(),
                customer.zipCode(),
                customer.city(),
                customer.country(),
                customer.tariffId(),
                customer.allowCredit(),
                customer.creditLimit(),
                customer.surchargeApply(),
                customer.notes(),
                customer.isActive(),
                customer.createdAt(),
                customer.updatedAt());

        newCustomer.setType(customer.getType());
        newCustomer.setOrigin(customer.getOrigin());
        newCustomer.setTags(customer.getTags());
        newCustomer.setMarketingConsent(customer.isMarketingConsent());
        newCustomer.setMarketingStatus(customer.getMarketingStatus());
        newCustomer.setUnsubscribeToken(customer.getUnsubscribeToken());
        newCustomer.setLastInteractionAt(customer.getLastInteractionAt());
        newCustomer.setSnoozedUntil(customer.getSnoozedUntil());

        return newCustomer;
    }

    private Customer update(Customer customer) {
        jdbcClient.sql("""
                UPDATE customers SET
                    tax_id = :taxId,
                    legal_name = :legalName,
                    commercial_name = :commercialName,
                    email = :email,
                    phone = :phone,
                    address = :address,
                    zip_code = :zipCode,
                    city = :city,
                    tariff_id = :tariffId,
                    allow_credit = :allowCredit,
                    credit_limit = :creditLimit,
                    surcharge_apply = :surchargeApply,
                    notes = :notes,
                    active = :active,
                    updated_at = :updatedAt,
                    type = :type,
                    origin = :origin,
                    tags = :tags,
                    marketing_consent = :marketingConsent,
                    marketing_status = :marketingStatus,
                    unsubscribe_token = :unsubscribeToken,
                    last_interaction_at = :lastInteractionAt,
                    snoozed_until = :snoozedUntil
                WHERE id = :id
                """)
                .param("id", customer.id().value())
                .param("taxId", customer.taxId() != null ? customer.taxId().value() : null)
                .param("legalName", customer.legalName())
                .param("commercialName", customer.commercialName())
                .param("email", customer.email() != null ? customer.email().value() : null)
                .param("phone", customer.phone())
                .param("address", customer.address())
                .param("zipCode", customer.zipCode())
                .param("city", customer.city())
                .param("tariffId", customer.tariffId())
                .param("allowCredit", customer.allowCredit())
                .param("creditLimit", customer.creditLimit().cents())
                .param("surchargeApply", customer.surchargeApply())
                .param("notes", customer.notes())
                .param("active", customer.isActive())
                .param("updatedAt", Timestamp.from(customer.updatedAt()))
                .param("type", customer.getType())
                .param("origin", customer.getOrigin())
                .param("tags", customer.getTags())
                .param("marketingConsent", customer.isMarketingConsent())
                .param("marketingStatus", customer.getMarketingStatus())
                .param("unsubscribeToken", customer.getUnsubscribeToken())
                .param("lastInteractionAt",
                        customer.getLastInteractionAt() != null ? Timestamp.from(customer.getLastInteractionAt())
                                : null)
                .param("snoozedUntil",
                        customer.getSnoozedUntil() != null ? Timestamp.from(customer.getSnoozedUntil()) : null)
                .update();

        return customer;
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jdbcClient.sql("SELECT * FROM customers WHERE id = :id")
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<Customer> findByUuid(UUID uuid) {
        return jdbcClient.sql("SELECT * FROM customers WHERE uuid = :uuid")
                .param("uuid", uuid)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Customer> findByCompanyId(CompanyId companyId) {
        return jdbcClient.sql("""
                SELECT * FROM customers WHERE company_id = :companyId AND active = TRUE
                ORDER BY legal_name
                """)
                .param("companyId", companyId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public List<Customer> searchCustomers(CompanyId companyId, String searchTerm) {
        return jdbcClient.sql("""
                SELECT * FROM customers
                WHERE company_id = :companyId
                AND active = TRUE
                AND (LOWER(legal_name) LIKE LOWER(:search) OR tax_id LIKE :search OR phone LIKE :search)
                ORDER BY legal_name
                LIMIT 50
                """)
                .param("companyId", companyId.value())
                .param("search", "%" + searchTerm + "%")
                .query(this::mapRow)
                .list();
    }

    @Override
    public List<Customer> findByMarketingCriteria(List<String> tags, String customerType,
            java.math.BigDecimal minSpent) {
        StringBuilder sql = new StringBuilder("SELECT * FROM customers WHERE active = TRUE");
        List<Object> params = new java.util.ArrayList<>();

        if (customerType != null) {
            sql.append(" AND type = ?");
            params.add(customerType);
        }

        if (tags != null && !tags.isEmpty()) {
            sql.append(" AND tags && ?::text[]");
            params.add(tags.toArray(new String[0]));
        }

        return jdbcClient.sql(sql.toString())
                .params(params)
                .query(this::mapRow)
                .list();
    }

    @Override
    public Optional<Customer> findByUnsubscribeToken(String token) {
        return jdbcClient.sql("SELECT * FROM customers WHERE unsubscribe_token = :token")
                .param("token", token)
                .query(this::mapRow)
                .optional();
    }

    private Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
        String taxIdStr = rs.getString("tax_id");
        String emailStr = rs.getString("email");

        Customer c = new Customer(
                new CustomerId(rs.getLong("id")),
                (UUID) rs.getObject("uuid"),
                new CompanyId((UUID) rs.getObject("company_id")),
                taxIdStr != null ? TaxId.of(taxIdStr) : null,
                rs.getString("legal_name"),
                rs.getString("commercial_name"),
                emailStr != null ? Email.of(emailStr) : null,
                rs.getString("phone"),
                rs.getString("address"),
                rs.getString("zip_code"),
                rs.getString("city"),
                rs.getString("country"),
                (Long) rs.getObject("tariff_id"),
                rs.getBoolean("allow_credit"),
                Money.ofEurosCents(rs.getLong("credit_limit")),
                rs.getBoolean("surcharge_apply"),
                rs.getString("notes"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());

        c.setType(rs.getString("type"));
        c.setOrigin(rs.getString("origin"));
        java.sql.Array tagsArray = rs.getArray("tags");
        if (tagsArray != null) {
            c.setTags((String[]) tagsArray.getArray());
        }
        c.setMarketingConsent(rs.getBoolean("marketing_consent"));
        c.setMarketingStatus(rs.getString("marketing_status"));
        c.setUnsubscribeToken(rs.getString("unsubscribe_token"));
        if (rs.getTimestamp("last_interaction_at") != null) {
            c.setLastInteractionAt(rs.getTimestamp("last_interaction_at").toInstant());
        }
        if (rs.getTimestamp("snoozed_until") != null) {
            c.setSnoozedUntil(rs.getTimestamp("snoozed_until").toInstant());
        }

        return c;
    }
}
