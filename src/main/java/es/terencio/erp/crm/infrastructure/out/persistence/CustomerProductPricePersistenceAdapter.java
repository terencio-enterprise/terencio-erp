package es.terencio.erp.crm.infrastructure.out.persistence;

import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import es.terencio.erp.crm.application.port.out.CustomerProductPriceRepositoryPort;
import es.terencio.erp.crm.domain.model.CustomerProductPrice;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

@Repository
public class CustomerProductPricePersistenceAdapter implements CustomerProductPriceRepositoryPort {

    private final JdbcClient jdbcClient;

    public CustomerProductPricePersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public CustomerProductPrice save(CustomerProductPrice price) {
        boolean exists = jdbcClient.sql("SELECT count(*) FROM customer_product_prices WHERE customer_id = ? AND product_id = ?")
                                   .param(price.customerId().value()).param(price.productId().value()).query(Integer.class).single() > 0;
        if (!exists) {
            jdbcClient.sql("INSERT INTO customer_product_prices (customer_id, product_id, custom_price, valid_from, created_at) VALUES (?, ?, ?, ?, ?)")
                .param(price.customerId().value())
                .param(price.productId().value())
                .param(price.customPrice().cents())
                .param(price.validFrom() != null ? java.sql.Timestamp.from(price.validFrom()) : null)
                .param(price.createdAt() != null ? java.sql.Timestamp.from(price.createdAt()) : null)
                .update();
        } else {
             jdbcClient.sql("UPDATE customer_product_prices SET custom_price = ? WHERE customer_id = ? AND product_id = ?")
                .param(price.customPrice().cents())
                .param(price.customerId().value())
                .param(price.productId().value())
                .update();
        }
        return price;
    }

    @Override
    public List<CustomerProductPrice> findByCustomerId(CustomerId customerId) {
        return jdbcClient.sql("SELECT * FROM customer_product_prices WHERE customer_id = ?")
            .param(customerId.value())
            .query((rs, rowNum) -> new CustomerProductPrice(
                new CustomerId(rs.getLong("customer_id")),
                new ProductId(rs.getLong("product_id")),
                Money.ofEurosCents(rs.getLong("custom_price")),
                rs.getTimestamp("valid_from") != null ? rs.getTimestamp("valid_from").toInstant() : null,
                rs.getTimestamp("valid_until") != null ? rs.getTimestamp("valid_until").toInstant() : null,
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toInstant() : null
            )).list();
    }
}