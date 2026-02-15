package es.terencio.erp.catalog.infrastructure.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import es.terencio.erp.catalog.application.port.out.CategoryRepository;
import es.terencio.erp.catalog.domain.model.Category;
import es.terencio.erp.shared.domain.identifier.CompanyId;

/**
 * JDBC adapter for Category persistence.
 */
@Repository
public class JdbcCategoryPersistenceAdapter implements CategoryRepository {

    private final JdbcClient jdbcClient;

    public JdbcCategoryPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Category save(Category category) {
        if (category.id() == null) {
            return insert(category);
        } else {
            return update(category);
        }
    }

    private Category insert(Category category) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcClient.sql("""
                INSERT INTO categories (company_id, parent_id, name, color, image_url, active)
                VALUES (:companyId, :parentId, :name, :color, :imageUrl, :active)
                RETURNING id
                """)
                .param("companyId", category.companyId().value())
                .param("parentId", category.parentId())
                .param("name", category.name())
                .param("color", category.color())
                .param("imageUrl", category.imageUrl())
                .param("active", category.isActive())
                .update(keyHolder);

        Long generatedId = ((Number) keyHolder.getKeys().get("id")).longValue();

        return new Category(
                generatedId,
                category.companyId(),
                category.parentId(),
                category.name(),
                category.color(),
                category.imageUrl(),
                category.isActive());
    }

    private Category update(Category category) {
        jdbcClient.sql("""
                UPDATE categories SET name = :name, color = :color, image_url = :imageUrl, active = :active
                WHERE id = :id
                """)
                .param("id", category.id())
                .param("name", category.name())
                .param("color", category.color())
                .param("imageUrl", category.imageUrl())
                .param("active", category.isActive())
                .update();

        return category;
    }

    @Override
    public Optional<Category> findById(Long id) {
        return jdbcClient.sql("SELECT * FROM categories WHERE id = :id")
                .param("id", id)
                .query(this::mapRow)
                .optional();
    }

    @Override
    public List<Category> findByCompanyId(CompanyId companyId) {
        return jdbcClient.sql("SELECT * FROM categories WHERE company_id = :companyId ORDER BY name")
                .param("companyId", companyId.value())
                .query(this::mapRow)
                .list();
    }

    private Category mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Category(
                rs.getLong("id"),
                new CompanyId((UUID) rs.getObject("company_id")),
                (Long) rs.getObject("parent_id"),
                rs.getString("name"),
                rs.getString("color"),
                rs.getString("image_url"),
                rs.getBoolean("active"));
    }
}
