package es.terencio.erp.crm.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.crm.application.port.out.CustomerProductPriceRepository;
import es.terencio.erp.crm.domain.model.CustomerProductPrice;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * JDBC adapter for CustomerProductPrice persistence.
 */
@Repository
public class JdbcCustomerProductPricePersistenceAdapter implements CustomerProductPriceRepository {

    private final JdbcClient jdbcClient;

    public JdbcCustomerProductPricePersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(CustomerProductPrice price) {
        jdbcClient
                .sql("""
                        INSERT INTO customer_product_prices (customer_id, product_id, custom_price, valid_from, valid_until, created_at)
                        VALUES (:customerId, :productId, :customPrice, :validFrom, :validUntil, :createdAt)
                        ON CONFLICT (customer_id, product_id) DO UPDATE SET
                            custom_price = EXCLUDED.custom_price,
                            valid_from = EXCLUDED.valid_from,
                            valid_until = EXCLUDED.valid_until
                        """)
                .param("customerId", price.customerId().value())
                .param("productId", price.productId().value())
                .param("customPrice", price.customPrice().cents())
                .param("validFrom", price.validFrom())
                .param("validUntil", price.validUntil())
                .param("createdAt", price.createdAt())
                .update();
    }

    @Override
    public Optional<CustomerProductPrice> findByCustomerAndProduct(CustomerId customerId, ProductId productId) {
        return jdbcClient.sql("""
                SELECT * FROM customer_product_prices
                WHERE customer_id = :customerId AND product_id = :productId
                """)
                .param("customerId", customerId.value())
                .param("productId", productId.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<CustomerProductPrice> findByCustomerId(CustomerId customerId) {
        return jdbcClient.sql("SELECT * FROM customer_product_prices WHERE customer_id = :customerId")
                .param("customerId", customerId.value())
                .query(this::mapRow)
                .list();
    }

    private CustomerProductPrice mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp validUntilTs = rs.getTimestamp("valid_until");
        return new CustomerProductPrice(
                new CustomerId(rs.getLong("customer_id")),
                new ProductId(rs.getLong("product_id")),
                Money.ofEurosCents(rs.getLong("custom_price")),
                rs.getTimestamp("valid_from").toInstant(),
                validUntilTs != null ? validUntilTs.toInstant() : null,
                rs.getTimestamp("created_at").toInstant());
    }
}
