package es.terencio.erp.crm.infrastructure.out.persistence;

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
        boolean exists = jdbcClient.sql("SELECT count(*) FROM customers WHERE uuid = ?")
                                   .param(customer.uuid()).query(Integer.class).single() > 0;
        if (!exists) {
            jdbcClient.sql("INSERT INTO customers (uuid, company_id, legal_name, tax_id, email, phone, address, zip_code, city, tariff_id, allow_credit, credit_limit, marketing_status, unsubscribe_token, type, active, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
                .param(customer.uuid())
                .param(customer.companyId().value())
                .param(customer.legalName())
                .param(customer.taxId() != null ? customer.taxId().value() : null)
                .param(customer.email() != null ? customer.email().value() : null)
                .param(customer.phone())
                .param(customer.address())
                .param(customer.zipCode())
                .param(customer.city())
                .param(customer.tariffId())
                .param(customer.allowCredit())
                .param(customer.creditLimit() != null ? customer.creditLimit().cents() : 0)
                .param(customer.getMarketingStatus() != null ? customer.getMarketingStatus().name() : "SUBSCRIBED")
                .param(customer.getUnsubscribeToken())
                .param(customer.getType() != null ? customer.getType().name() : "CLIENT")
                .param(customer.isActive())
                .param(java.sql.Timestamp.from(customer.createdAt()))
                .param(java.sql.Timestamp.from(customer.updatedAt()))
                .update();
        } else {
             jdbcClient.sql("UPDATE customers SET email = ?, phone = ?, address = ?, zip_code = ?, city = ?, tariff_id = ?, allow_credit = ?, credit_limit = ?, marketing_status = ?, updated_at = ? WHERE uuid = ?")
                .param(customer.email() != null ? customer.email().value() : null)
                .param(customer.phone())
                .param(customer.address())
                .param(customer.zipCode())
                .param(customer.city())
                .param(customer.tariffId())
                .param(customer.allowCredit())
                .param(customer.creditLimit() != null ? customer.creditLimit().cents() : 0)
                .param(customer.getMarketingStatus() != null ? customer.getMarketingStatus().name() : "SUBSCRIBED")
                .param(java.sql.Timestamp.from(customer.updatedAt()))
                .param(customer.uuid())
                .update();
        }
        return customer;
    }

    @Override
    public Optional<Customer> findByUuid(UUID uuid) {
        return jdbcClient.sql("SELECT * FROM customers WHERE uuid = ?").param(uuid).query(this::mapRow).optional();
    }

    @Override
    public List<Customer> findByCompanyId(CompanyId companyId) {
        return jdbcClient.sql("SELECT * FROM customers WHERE company_id = ?").param(companyId.value()).query(this::mapRow).list();
    }

    @Override
    public List<Customer> searchCustomers(CompanyId companyId, String search) {
        return jdbcClient.sql("SELECT * FROM customers WHERE company_id = ? AND (legal_name ILIKE ? OR email ILIKE ?)")
            .param(companyId.value()).param("%" + search + "%").param("%" + search + "%").query(this::mapRow).list();
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
            rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null,
            rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toInstant() : null,
            rs.getTimestamp("deleted_at") != null ? rs.getTimestamp("deleted_at").toInstant() : null
        );
        if (rs.getString("type") != null) c.setType(CustomerType.valueOf(rs.getString("type")));
        if (rs.getString("marketing_status") != null) c.setMarketingStatus(MarketingStatus.valueOf(rs.getString("marketing_status")));
        c.setUnsubscribeToken(rs.getString("unsubscribe_token"));
        return c;
    }
}