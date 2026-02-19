package es.terencio.erp.crm.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.crm.application.port.out.CustomerProductPriceRepository;
import es.terencio.erp.crm.domain.model.CustomerProductPrice;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

@Repository
public class CustomerProductPriceJdbcAdapter implements CustomerProductPriceRepository {

    private final JdbcClient jdbcClient;

    public CustomerProductPriceJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Upserts a customer product price.
     * The composite PK is (customer_id, product_id).
     */
    @Override
    public void save(CustomerProductPrice price) {
        String sql = """
                INSERT INTO customer_product_prices (customer_id, product_id, custom_price, valid_from, valid_until, created_at)
                VALUES (:customerId, :productId, :customPrice, :validFrom, :validUntil, :createdAt)
                ON CONFLICT (customer_id, product_id) DO UPDATE SET
                    custom_price = EXCLUDED.custom_price,
                    valid_from   = EXCLUDED.valid_from,
                    valid_until  = EXCLUDED.valid_until
                """;

        jdbcClient.sql(sql)
                .param("customerId", price.customerId().value())
                .param("productId", price.productId().value())
                .param("customPrice", price.customPrice().cents())
                .param("validFrom", price.validFrom())
                .param("validUntil", price.validUntil())
                .param("createdAt", price.createdAt())
                .update();
    }

    // -------------------------------------------------------------------------
    // Read operations
    // -------------------------------------------------------------------------

    @Override
    public Optional<CustomerProductPrice> findByCustomerAndProduct(CustomerId customerId, ProductId productId) {
        String sql = """
                SELECT * FROM customer_product_prices
                WHERE customer_id = :customerId
                  AND product_id  = :productId
                """;

        return jdbcClient.sql(sql)
                .param("customerId", customerId.value())
                .param("productId", productId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<CustomerProductPrice> findByCustomerId(CustomerId customerId) {
        String sql = """
                SELECT * FROM customer_product_prices
                WHERE customer_id = :customerId
                ORDER BY product_id ASC
                """;

        return jdbcClient.sql(sql)
                .param("customerId", customerId.value())
                .query(this::mapRow)
                .list();
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private CustomerProductPrice mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long customerId = rs.getLong("customer_id");
        Long productId = rs.getLong("product_id");
        long priceCents = rs.getLong("custom_price");
        Instant validFrom = rs.getTimestamp("valid_from").toInstant();
        var validUntilTs = rs.getTimestamp("valid_until");
        Instant validUntil = validUntilTs != null ? validUntilTs.toInstant() : null;
        Instant createdAt = rs.getTimestamp("created_at").toInstant();

        return new CustomerProductPrice(
                new CustomerId(customerId),
                new ProductId(productId),
                Money.ofEurosCents(priceCents),
                validFrom,
                validUntil,
                createdAt);
    }
}
