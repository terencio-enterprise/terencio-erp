package es.terencio.erp.marketing.infrastructure.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import es.terencio.erp.marketing.application.port.out.AssetRepositoryPort;
import es.terencio.erp.marketing.domain.model.CompanyAsset;

@Repository
public class JdbcAssetRepository implements AssetRepositoryPort {

    private final JdbcClient jdbcClient;

    public JdbcAssetRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public CompanyAsset save(CompanyAsset asset) {
        String sql = """
                INSERT INTO company_assets (id, company_id, filename, content_type, file_size_bytes, storage_path, public_url, is_public, created_at, updated_at)
                VALUES (:id, :companyId, :filename, :contentType, :fileSizeBytes, :storagePath, :publicUrl, :isPublic, :createdAt, :updatedAt)
                ON CONFLICT (id) DO UPDATE SET
                    filename = EXCLUDED.filename,
                    is_public = EXCLUDED.is_public,
                    updated_at = NOW()
                """;

        jdbcClient.sql(sql)
                .param("id", asset.getId())
                .param("companyId", asset.getCompanyId())
                .param("filename", asset.getFilename())
                .param("contentType", asset.getContentType())
                .param("fileSizeBytes", asset.getFileSizeBytes())
                .param("storagePath", asset.getStoragePath())
                .param("publicUrl", asset.getPublicUrl())
                .param("isPublic", asset.isPublic())
                .param("createdAt", asset.getCreatedAt())
                .param("updatedAt", asset.getUpdatedAt())
                .update();

        return asset;
    }

    @Override
    public Optional<CompanyAsset> findByIdAndCompanyId(UUID id, UUID companyId) {
        String sql = "SELECT * FROM company_assets WHERE id = :id AND company_id = :companyId";
        return jdbcClient.sql(sql)
                .param("id", id)
                .param("companyId", companyId)
                .query(CompanyAsset.class)
                .optional();
    }

    @Override
    public void deleteById(UUID id) {
        jdbcClient.sql("DELETE FROM company_assets WHERE id = :id")
                .param("id", id)
                .update();
    }

    @Override
    public long countByFilters(UUID companyId, String search, String contentType) {
        String sql = """
                SELECT COUNT(*) FROM company_assets
                WHERE company_id = :companyId
                AND (:search IS NULL OR filename ILIKE '%' || :search || '%')
                AND (:contentType IS NULL OR content_type = :contentType)
                """;

        return jdbcClient.sql(sql)
                .param("companyId", companyId)
                .param("search", search == null || search.isBlank() ? null : search)
                .param("contentType", contentType == null || contentType.isBlank() ? null : contentType)
                .query(Long.class)
                .single();
    }

    @Override
    public List<CompanyAsset> findByFiltersPaginated(UUID companyId, String search, String contentType, int offset,
            int limit) {
        String sql = """
                SELECT * FROM company_assets
                WHERE company_id = :companyId
                AND (:search IS NULL OR filename ILIKE '%' || :search || '%')
                AND (:contentType IS NULL OR content_type = :contentType)
                ORDER BY created_at DESC
                LIMIT :limit OFFSET :offset
                """;

        return jdbcClient.sql(sql)
                .param("companyId", companyId)
                .param("search", search == null || search.isBlank() ? null : search)
                .param("contentType", contentType == null || contentType.isBlank() ? null : contentType)
                .param("limit", limit)
                .param("offset", offset)
                .query(CompanyAsset.class)
                .list();
    }
}