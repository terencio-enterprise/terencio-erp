package es.terencio.erp.catalog.infrastructure.persistence;

import java.math.BigDecimal;
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

import es.terencio.erp.catalog.application.port.out.ProductRepository;
import es.terencio.erp.catalog.domain.model.Product;
import es.terencio.erp.catalog.domain.model.ProductType;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * JDBC adapter for Product persistence.
 */
@Repository
public class JdbcProductPersistenceAdapter implements ProductRepository {

    private final JdbcClient jdbcClient;

    public JdbcProductPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Product save(Product product) {
        if (product.id() == null) {
            return insert(product);
        } else {
            return update(product);
        }
    }

    private Product insert(Product product) {
        UUID uuid = product.uuid() != null ? product.uuid() : UUID.randomUUID();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql("""
                INSERT INTO products (uuid, company_id, reference, name, short_name, description,
                    category_id, tax_id, brand, type, is_weighted, is_inventoriable,
                    min_stock_alert, average_cost, last_purchase_cost, image_url, active,
                    created_at, updated_at, version)
                VALUES (:uuid, :companyId, :reference, :name, :shortName, :description,
                    :categoryId, :taxId, :brand, :type, :isWeighted, :isInventoriable,
                    :minStockAlert, :averageCost, :lastPurchaseCost, :imageUrl, :active,
                    :createdAt, :updatedAt, :version)
                RETURNING id
                """)
                .param("uuid", uuid)
                .param("companyId", product.companyId().value())
                .param("reference", product.reference())
                .param("name", product.name())
                .param("shortName", product.shortName())
                .param("description", product.description())
                .param("categoryId", product.categoryId())
                .param("taxId", product.taxId())
                .param("brand", product.brand())
                .param("type", product.type().name())
                .param("isWeighted", product.isWeighted())
                .param("isInventoriable", product.isInventoriable())
                .param("minStockAlert", product.minStockAlert())
                .param("averageCost", product.averageCost().cents())
                .param("lastPurchaseCost", product.lastPurchaseCost().cents())
                .param("imageUrl", product.imageUrl())
                .param("active", product.isActive())
                .param("createdAt", Timestamp.from(product.createdAt()))
                .param("updatedAt", Timestamp.from(product.updatedAt()))
                .param("version", product.version())
                .update(keyHolder);

        Long generatedId = ((Number) keyHolder.getKeys().get("id")).longValue();

        return new Product(
                new ProductId(generatedId),
                uuid,
                product.companyId(),
                product.reference(),
                product.name(),
                product.shortName(),
                product.description(),
                product.categoryId(),
                product.taxId(),
                product.brand(),
                product.type(),
                product.isWeighted(),
                product.isInventoriable(),
                product.minStockAlert(),
                product.averageCost(),
                product.lastPurchaseCost(),
                product.imageUrl(),
                product.isActive(),
                product.createdAt(),
                product.updatedAt(),
                product.version());
    }

    private Product update(Product product) {
        jdbcClient.sql("""
                UPDATE products SET
                    name = :name,
                    short_name = :shortName,
                    description = :description,
                    category_id = :categoryId,
                    tax_id = :taxId,
                    brand = :brand,
                    min_stock_alert = :minStockAlert,
                    average_cost = :averageCost,
                    last_purchase_cost = :lastPurchaseCost,
                    image_url = :imageUrl,
                    active = :active,
                    updated_at = :updatedAt,
                    version = :version
                WHERE id = :id
                """)
                .param("id", product.id().value())
                .param("name", product.name())
                .param("shortName", product.shortName())
                .param("description", product.description())
                .param("categoryId", product.categoryId())
                .param("taxId", product.taxId())
                .param("brand", product.brand())
                .param("minStockAlert", product.minStockAlert())
                .param("averageCost", product.averageCost().cents())
                .param("lastPurchaseCost", product.lastPurchaseCost().cents())
                .param("imageUrl", product.imageUrl())
                .param("active", product.isActive())
                .param("updatedAt", Timestamp.from(product.updatedAt()))
                .param("version", product.version())
                .update();

        return product;
    }

    @Override
    public Optional<Product> findById(ProductId id) {
        return jdbcClient.sql("SELECT * FROM products WHERE id = :id")
                .param("id", id.value())
                .query(this::mapRow)
                .optional();
    }

    @Override
    public Optional<Product> findByCompanyAndReference(CompanyId companyId, String reference) {
        return jdbcClient.sql("""
                SELECT * FROM products WHERE company_id = :companyId AND reference = :reference
                """)
                .param("companyId", companyId.value())
                .param("reference", reference)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public boolean existsByCompanyAndReference(CompanyId companyId, String reference) {
        Integer count = jdbcClient.sql("""
                SELECT COUNT(*) FROM products WHERE company_id = :companyId AND reference = :reference
                """)
                .param("companyId", companyId.value())
                .param("reference", reference)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    public List<Product> findByCompanyId(CompanyId companyId, int page, int size) {
        int offset = page * size;
        return jdbcClient.sql("""
                SELECT * FROM products
                WHERE company_id = :companyId
                ORDER BY name
                LIMIT :limit OFFSET :offset
                """)
                .param("companyId", companyId.value())
                .param("limit", size)
                .param("offset", offset)
                .query(this::mapRow)
                .list();
    }

    public List<Product> searchProducts(CompanyId companyId, String name, String reference, Long categoryId, int page,
            int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM products WHERE company_id = :companyId");

        if (name != null && !name.isBlank()) {
            sql.append(" AND LOWER(name) LIKE LOWER(:name)");
        }
        if (reference != null && !reference.isBlank()) {
            sql.append(" AND reference = :reference");
        }
        if (categoryId != null) {
            sql.append(" AND category_id = :categoryId");
        }

        sql.append(" ORDER BY name LIMIT :limit OFFSET :offset");

        JdbcClient.StatementSpec spec = jdbcClient.sql(sql.toString())
                .param("companyId", companyId.value())
                .param("limit", size)
                .param("offset", page * size);

        if (name != null && !name.isBlank()) {
            spec = spec.param("name", "%" + name + "%");
        }
        if (reference != null && !reference.isBlank()) {
            spec = spec.param("reference", reference);
        }
        if (categoryId != null) {
            spec = spec.param("categoryId", categoryId);
        }

        return spec.query(this::mapRow).list();
    }

    private Product mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Product(
                new ProductId(rs.getLong("id")),
                (UUID) rs.getObject("uuid"),
                new CompanyId((UUID) rs.getObject("company_id")),
                rs.getString("reference"),
                rs.getString("name"),
                rs.getString("short_name"),
                rs.getString("description"),
                (Long) rs.getObject("category_id"),
                (Long) rs.getObject("tax_id"),
                rs.getString("brand"),
                ProductType.valueOf(rs.getString("type")),
                rs.getBoolean("is_weighted"),
                rs.getBoolean("is_inventoriable"),
                (BigDecimal) rs.getObject("min_stock_alert"),
                Money.ofEurosCents(rs.getLong("average_cost")),
                Money.ofEurosCents(rs.getLong("last_purchase_cost")),
                rs.getString("image_url"),
                rs.getBoolean("active"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant(),
                rs.getLong("version"));
    }
}
