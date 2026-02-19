package es.terencio.erp.crm.infrastructure.out.persistence;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.terencio.erp.crm.application.port.out.CustomerRepository;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;
import es.terencio.erp.crm.domain.model.MarketingStatus;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.TaxId;

@Repository
public class CustomerJdbcAdapter implements CustomerRepository {

    private final JdbcClient jdbcClient;

    public CustomerJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    @Override
    public Customer save(Customer customer) {
        if (customer.id() == null) {
            return insert(customer);
        } else {
            update(customer);
            return customer;
        }
    }

    private Customer insert(Customer customer) {
        String sql = """
                INSERT INTO customers (
                    uuid, company_id, tax_id, legal_name, commercial_name,
                    email, phone, address, zip_code, city, country,
                    tariff_id, allow_credit, credit_limit, surcharge_apply, notes,
                    active, created_at, updated_at, deleted_at,
                    type, origin, tags, marketing_consent, marketing_status,
                    unsubscribe_token, last_interaction_at, snoozed_until
                ) VALUES (
                    :uuid, :companyId, :taxId, :legalName, :commercialName,
                    :email, :phone, :address, :zipCode, :city, :country,
                    :tariffId, :allowCredit, :creditLimit, :surchargeApply, :notes,
                    :active, :createdAt, :updatedAt, :deletedAt,
                    :type, :origin, :tags, :marketingConsent, :marketingStatus,
                    :unsubscribeToken, :lastInteractionAt, :snoozedUntil
                )
                """;

        KeyHolder keyHolder = new GeneratedKeyHolder();

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
                .param("creditLimit", customer.creditLimit().cents())
                .param("surchargeApply", customer.surchargeApply())
                .param("notes", customer.notes())
                .param("active", customer.isActive())
                .param("createdAt", customer.createdAt())
                .param("updatedAt", customer.updatedAt())
                .param("deletedAt", customer.deletedAt())
                .param("type", customer.getType() != null ? customer.getType().name() : CustomerType.CLIENT.name())
                .param("origin", customer.getOrigin())
                .param("tags", customer.getTags() != null ? customer.getTags().toArray(new String[0]) : new String[0])
                .param("marketingConsent", customer.isMarketingConsent())
                .param("marketingStatus",
                        customer.getMarketingStatus() != null ? customer.getMarketingStatus().name()
                                : MarketingStatus.SUBSCRIBED.name())
                .param("unsubscribeToken", customer.getUnsubscribeToken())
                .param("lastInteractionAt", customer.getLastInteractionAt())
                .param("snoozedUntil", customer.getSnoozedUntil())
                .update(keyHolder, "id");

        Long generatedId = keyHolder.getKeyAs(Long.class);

        return new Customer(
                new CustomerId(generatedId),
                customer.uuid(), customer.companyId(), customer.taxId(), customer.legalName(),
                customer.commercialName(), customer.email(), customer.phone(), customer.address(),
                customer.zipCode(), customer.city(), customer.country(), customer.tariffId(),
                customer.allowCredit(), customer.creditLimit(), customer.surchargeApply(),
                customer.notes(), customer.isActive(), customer.createdAt(), customer.updatedAt(),
                customer.deletedAt());
    }

    private void update(Customer customer) {
        String sql = """
                UPDATE customers SET
                    tax_id               = :taxId,
                    legal_name           = :legalName,
                    commercial_name      = :commercialName,
                    email                = :email,
                    phone                = :phone,
                    address              = :address,
                    zip_code             = :zipCode,
                    city                 = :city,
                    country              = :country,
                    tariff_id            = :tariffId,
                    allow_credit         = :allowCredit,
                    credit_limit         = :creditLimit,
                    surcharge_apply      = :surchargeApply,
                    notes                = :notes,
                    active               = :active,
                    updated_at           = :updatedAt,
                    deleted_at           = :deletedAt,
                    type                 = :type,
                    origin               = :origin,
                    tags                 = :tags,
                    marketing_consent    = :marketingConsent,
                    marketing_status     = :marketingStatus,
                    unsubscribe_token    = :unsubscribeToken,
                    last_interaction_at  = :lastInteractionAt,
                    snoozed_until        = :snoozedUntil
                WHERE id = :id
                """;

        jdbcClient.sql(sql)
                .param("id", customer.id().value())
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
                .param("updatedAt", customer.updatedAt())
                .param("deletedAt", customer.deletedAt())
                .param("type", customer.getType() != null ? customer.getType().name() : CustomerType.CLIENT.name())
                .param("origin", customer.getOrigin())
                .param("tags", customer.getTags() != null ? customer.getTags().toArray(new String[0]) : new String[0])
                .param("marketingConsent", customer.isMarketingConsent())
                .param("marketingStatus",
                        customer.getMarketingStatus() != null ? customer.getMarketingStatus().name()
                                : MarketingStatus.SUBSCRIBED.name())
                .param("unsubscribeToken", customer.getUnsubscribeToken())
                .param("lastInteractionAt", customer.getLastInteractionAt())
                .param("snoozedUntil", customer.getSnoozedUntil())
                .update();
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jdbcClient.sql("SELECT * FROM customers WHERE id = :id AND deleted_at IS NULL")
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<Customer> findByUuid(UUID uuid) {
        return jdbcClient.sql("SELECT * FROM customers WHERE uuid = :uuid AND deleted_at IS NULL")
                .param("uuid", uuid)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Customer> findByCompanyId(CompanyId companyId) {
        return jdbcClient.sql("""
                SELECT * FROM customers
                WHERE company_id = :companyId AND deleted_at IS NULL
                ORDER BY legal_name ASC
                """)
                .param("companyId", companyId.value())
                .query(this::mapRow)
                .list();
    }

    @Override
    public List<Customer> searchCustomers(CompanyId companyId, String searchTerm) {
        String like = "%" + searchTerm + "%";
        return jdbcClient.sql("""
                SELECT * FROM customers
                WHERE company_id = :companyId
                  AND deleted_at IS NULL
                  AND (legal_name ILIKE :term OR commercial_name ILIKE :term OR email ILIKE :term)
                ORDER BY legal_name ASC
                """)
                .param("companyId", companyId.value())
                .param("term", like)
                .query(this::mapRow)
                .list();
    }

    @Override
    public List<Customer> findByMarketingCriteria(List<String> tags, String customerType, BigDecimal minSpent) {
        StringBuilder sql = new StringBuilder("""
                SELECT * FROM customers
                WHERE deleted_at IS NULL
                  AND marketing_consent = TRUE
                  AND marketing_status NOT IN ('UNSUBSCRIBED', 'BOUNCED', 'COMPLAINED')
                """);

        if (customerType != null) {
            sql.append("  AND type = :type\n");
        }
        if (tags != null && !tags.isEmpty()) {
            sql.append("  AND tags && :tags\n"); // PostgreSQL array overlap operator
        }

        var spec = jdbcClient.sql(sql.toString());

        if (customerType != null) {
            spec = spec.param("type", customerType);
        }
        if (tags != null && !tags.isEmpty()) {
            spec = spec.param("tags", tags.toArray(new String[0]));
        }

        return spec.query(this::mapRow).list();
    }

    @Override
    public Optional<Customer> findByUnsubscribeToken(String token) {
        return jdbcClient.sql("SELECT * FROM customers WHERE unsubscribe_token = :token")
                .param("token", token)
                .query(this::mapRow)
                .optional();
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long id = rs.getLong("id");
        UUID uuid = (UUID) rs.getObject("uuid");
        UUID companyUuid = (UUID) rs.getObject("company_id");
        String taxIdStr = rs.getString("tax_id");
        String legalName = rs.getString("legal_name");
        String commercialName = rs.getString("commercial_name");
        String emailStr = rs.getString("email");
        String phone = rs.getString("phone");
        String address = rs.getString("address");
        String zipCode = rs.getString("zip_code");
        String city = rs.getString("city");
        String country = rs.getString("country");
        Long tariffId = rs.getObject("tariff_id", Long.class);
        boolean allowCredit = rs.getBoolean("allow_credit");
        long creditCents = rs.getLong("credit_limit");
        boolean surcharge = rs.getBoolean("surcharge_apply");
        String notes = rs.getString("notes");
        boolean active = rs.getBoolean("active");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        var deletedAtTs = rs.getTimestamp("deleted_at");
        Instant deletedAt = deletedAtTs != null ? deletedAtTs.toInstant() : null;

        // Marketing fields
        String typeStr = rs.getString("type");
        String origin = rs.getString("origin");
        Array tagsArray = rs.getArray("tags");
        String marketingStatusStr = rs.getString("marketing_status");
        boolean marketingConsent = rs.getBoolean("marketing_consent");
        String unsubToken = rs.getString("unsubscribe_token");
        var lastIntTs = rs.getTimestamp("last_interaction_at");
        Instant lastInteraction = lastIntTs != null ? lastIntTs.toInstant() : null;
        var snoozedTs = rs.getTimestamp("snoozed_until");
        Instant snoozedUntil = snoozedTs != null ? snoozedTs.toInstant() : null;

        Customer customer = new Customer(
                new CustomerId(id),
                uuid,
                new CompanyId(companyUuid),
                taxIdStr != null ? TaxId.of(taxIdStr) : null,
                legalName,
                commercialName,
                emailStr != null ? Email.of(emailStr) : null,
                phone,
                address,
                zipCode,
                city,
                country,
                tariffId,
                allowCredit,
                Money.ofEurosCents(creditCents),
                surcharge,
                notes,
                active,
                createdAt,
                updatedAt,
                deletedAt);

        // Set marketing fields via setters
        if (typeStr != null)
            customer.setType(CustomerType.valueOf(typeStr));
        customer.setOrigin(origin);
        if (tagsArray != null) {
            String[] tagArr = (String[]) tagsArray.getArray();
            customer.setTags(Arrays.asList(tagArr));
        }
        customer.setMarketingConsent(marketingConsent);
        if (marketingStatusStr != null)
            customer.setMarketingStatus(MarketingStatus.valueOf(marketingStatusStr));
        customer.setUnsubscribeToken(unsubToken);
        customer.setLastInteractionAt(lastInteraction);
        customer.setSnoozedUntil(snoozedUntil);

        return customer;
    }
}
