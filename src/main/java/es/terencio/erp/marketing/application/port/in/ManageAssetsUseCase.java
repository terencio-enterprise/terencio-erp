package es.terencio.erp.marketing.application.port.in;

import java.io.InputStream;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.AssetResponse;
import es.terencio.erp.shared.domain.query.PageResult;

public interface ManageAssetsUseCase {
    AssetResponse uploadAsset(UUID companyId, String filename, String contentType, long sizeBytes,
            InputStream inputStream, boolean isPublic);

    AssetResponse getAsset(UUID companyId, UUID assetId);

    PageResult<AssetResponse> searchAssets(UUID companyId, String search, String contentType, int page, int size);

    void deleteAsset(UUID companyId, UUID assetId);
}