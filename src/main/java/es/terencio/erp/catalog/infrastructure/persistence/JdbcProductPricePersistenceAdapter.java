package es.terencio.erp.catalog.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.catalog.application.port.out.ProductPriceRepository;
import es.terencio.erp.catalog.domain.model.ProductPrice;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * JDBC adapter for ProductPrice persistence.
 */
@Repository
public class JdbcProductPricePersistenceAdapter implements ProductPriceRepository {

    private final JdbcClient jdbcClient;

    public JdbcProductPricePersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void save(ProductPrice productPrice) {
        jdbcClient.sql("""
                INSERT INTO product_prices (product_id, tariff_id, price, cost_price, updated_at)
                VALUES (:productId, :tariffId, :price, :costPrice, :updatedAt)
                ON CONFLICT (product_id, tariff_id) DO UPDATE SET
                    price = EXCLUDED.price,
                    cost_price = EXCLUDED.cost_price,
                    updated_at = EXCLUDED.updated_at
                """)
                .param("productId", productPrice.productId().value())
                .param("tariffId", productPrice.tariffId())
                .param("price", productPrice.price().cents())
                .param("costPrice", productPrice.costPrice() != null ? productPrice.costPrice().cents() : null)
                .param("updatedAt", Timestamp.from(productPrice.updatedAt()))
                .update();
    }

    @Override
    public Optional<ProductPrice> findByProductAndTariff(ProductId productId, Long tariffId) {
        return jdbcClient.sql("""
                SELECT * FROM product_prices WHERE product_id = :productId AND tariff_id = :tariffId
                """)
                .param("productId", productId.value())
                .param("tariffId", tariffId)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<ProductPrice> findByProductId(ProductId productId) {
        return jdbcClient.sql("SELECT * FROM product_prices WHERE product_id = :productId")
                .param("productId", productId.value())
                .query(this::mapRow)
                .list();
    }

    private ProductPrice mapRow(ResultSet rs, int rowNum) throws SQLException {
        Long costPriceCents = (Long) rs.getObject("cost_price");
        return new ProductPrice(
                new ProductId(rs.getLong("product_id")),
                rs.getLong("tariff_id"),
                Money.ofEurosCents(rs.getLong("price")),
                costPriceCents != null ? Money.ofEurosCents(costPriceCents) : null,
                rs.getTimestamp("updated_at").toInstant());
    }
}
