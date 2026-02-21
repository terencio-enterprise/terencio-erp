package es.terencio.erp.marketing.infrastructure.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import es.terencio.erp.marketing.application.port.out.AssetRepositoryPort;
import es.terencio.erp.marketing.domain.model.CompanyAsset;

@Repository
public class JdbcAssetRepository implements AssetRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcAssetRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<CompanyAsset> rowMapper = (rs, rowNum) -> mapRowToAsset(rs);

    @Override
    public CompanyAsset save(CompanyAsset asset) {
        String sql = """
            INSERT INTO company_assets (
                id, company_id, filename, content_type, file_size_bytes, 
                storage_path, public_url, is_public, created_at, updated_at
            ) VALUES (
                :id, :companyId, :filename, :contentType, :fileSizeBytes, 
                :storagePath, :publicUrl, :isPublic, :createdAt, :updatedAt
            )
            ON CONFLICT (id) DO UPDATE SET
                filename = EXCLUDED.filename,
                content_type = EXCLUDED.content_type,
                file_size_bytes = EXCLUDED.file_size_bytes,
                is_public = EXCLUDED.is_public,
                updated_at = NOW()
        """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", asset.getId())
            .addValue("companyId", asset.getCompanyId())
            .addValue("filename", asset.getFilename())
            .addValue("contentType", asset.getContentType())
            .addValue("fileSizeBytes", asset.getFileSizeBytes())
            .addValue("storagePath", asset.getStoragePath())
            .addValue("publicUrl", asset.getPublicUrl())
            .addValue("isPublic", asset.isPublic())
            .addValue("createdAt", asset.getCreatedAt() != null ? java.sql.Timestamp.from(asset.getCreatedAt()) : null)
            .addValue("updatedAt", java.sql.Timestamp.from(Instant.now()));

        jdbc.update(sql, params);
        return asset;
    }

    @Override
    public long countByFilters(UUID companyId, String search, String contentType) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM company_assets WHERE company_id = :companyId");
        MapSqlParameterSource params = new MapSqlParameterSource("companyId", companyId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND filename ILIKE :search");
            params.addValue("search", "%" + search + "%");
        }
        if (contentType != null && !contentType.isBlank()) {
            sql.append(" AND content_type = :contentType");
            params.addValue("contentType", contentType);
        }

        Long count = jdbc.queryForObject(sql.toString(), params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public List<CompanyAsset> findByFiltersPaginated(UUID companyId, String search, String contentType, int offset, int size) {
        StringBuilder sql = new StringBuilder("SELECT * FROM company_assets WHERE company_id = :companyId");
        MapSqlParameterSource params = new MapSqlParameterSource("companyId", companyId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND filename ILIKE :search");
            params.addValue("search", "%" + search + "%");
        }
        if (contentType != null && !contentType.isBlank()) {
            sql.append(" AND content_type = :contentType");
            params.addValue("contentType", contentType);
        }

        sql.append(" ORDER BY created_at DESC LIMIT :size OFFSET :offset");
        params.addValue("size", size).addValue("offset", offset);

        return jdbc.query(sql.toString(), params, rowMapper);
    }

    @Override
    public Optional<CompanyAsset> findByIdAndCompanyId(UUID id, UUID companyId) {
        String sql = "SELECT * FROM company_assets WHERE id = :id AND company_id = :companyId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", id)
            .addValue("companyId", companyId);
        
        List<CompanyAsset> results = jdbc.query(sql, params, rowMapper);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM company_assets WHERE id = :id", new MapSqlParameterSource("id", id));
    }

    private CompanyAsset mapRowToAsset(ResultSet rs) throws SQLException {
        CompanyAsset asset = new CompanyAsset(
            rs.getObject("company_id", UUID.class),
            rs.getString("filename"),
            rs.getString("content_type"),
            rs.getLong("file_size_bytes"),
            rs.getString("storage_path"),
            rs.getString("public_url"),
            rs.getBoolean("is_public")
        );
        

        try {
            var idField = CompanyAsset.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(asset, rs.getObject("id", UUID.class));

            var createdAtField = CompanyAsset.class.getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(asset, rs.getTimestamp("created_at").toInstant());
            
            var updatedAtField = CompanyAsset.class.getDeclaredField("updatedAt");
            updatedAtField.setAccessible(true);
            updatedAtField.set(asset, rs.getTimestamp("updated_at").toInstant());
        } catch (Exception e) {
            throw new RuntimeException("Failed to map CompanyAsset fields", e);
        }

        return asset;
    }
}