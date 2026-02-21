package es.terencio.erp.marketing.application.service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.AssetResponse;
import es.terencio.erp.marketing.application.port.in.ManageAssetsUseCase;
import es.terencio.erp.marketing.application.port.out.AssetRepositoryPort;
import es.terencio.erp.marketing.application.port.out.FileStoragePort;
import es.terencio.erp.marketing.application.port.out.FileStoragePort.StorageResult;
import es.terencio.erp.marketing.domain.model.CompanyAsset;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class AssetService implements ManageAssetsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AssetService.class);
    private final AssetRepositoryPort assetRepository;
    private final FileStoragePort fileStoragePort;

    public AssetService(AssetRepositoryPort assetRepository, FileStoragePort fileStoragePort) {
        this.assetRepository = assetRepository;
        this.fileStoragePort = fileStoragePort;
    }

    @Override
    @Transactional
    public AssetResponse uploadAsset(UUID companyId, String filename, String contentType, long sizeBytes,
            InputStream inputStream, boolean isPublic) {
        if (sizeBytes == 0 || filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Invalid file: name and size must be valid");
        }

        log.info("Uploading new asset {} for company {}", filename, companyId);
        StorageResult storageResult = fileStoragePort.upload(companyId, filename, contentType, sizeBytes, inputStream, isPublic);

        try {
            CompanyAsset asset = new CompanyAsset(
                    companyId,
                    filename,
                    contentType,
                    sizeBytes,
                    storageResult.storagePath(),
                    storageResult.publicUrl(),
                    isPublic);

            CompanyAsset saved = assetRepository.save(asset);
            return toDto(saved);
        } catch (Exception e) {
            log.error("Database save failed for asset {}, cleaning up storage", filename);
            fileStoragePort.delete(storageResult.storagePath());
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AssetResponse getAsset(UUID companyId, UUID assetId) {
        CompanyAsset asset = assetRepository.findByIdAndCompanyId(assetId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));
        return toDto(asset);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AssetResponse> searchAssets(UUID companyId, String search, String contentType, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        int offset = safePage * safeSize;
        
        String normSearch = search != null ? search.trim() : null;
        String normContentType = contentType != null ? contentType.trim() : null;
        
        long totalElements = assetRepository.countByFilters(companyId, normSearch, normContentType);
        List<CompanyAsset> assets = assetRepository.findByFiltersPaginated(companyId, normSearch, normContentType, offset, safeSize);

        List<AssetResponse> content = assets.stream().map(this::toDto).collect(Collectors.toList());
        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        return new PageResult<AssetResponse>(content, totalElements, totalPages, safePage, safeSize);
    }

    @Override
    @Transactional
    public void deleteAsset(UUID companyId, UUID assetId) {
        CompanyAsset asset = assetRepository.findByIdAndCompanyId(assetId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        log.info("Deleting asset {} for company {}", assetId, companyId);
        fileStoragePort.delete(asset.getStoragePath());
        assetRepository.deleteById(assetId);
    }

    private AssetResponse toDto(CompanyAsset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getFilename(),
                asset.getContentType(),
                asset.getFileSizeBytes(),
                asset.getPublicUrl(),
                asset.isPublic(),
                asset.getCreatedAt());
    }
}