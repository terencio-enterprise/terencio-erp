package es.terencio.erp.marketing.application.service;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.AssetDtos.AssetResponse;
import es.terencio.erp.marketing.application.dto.AssetDtos.PageDto;
import es.terencio.erp.marketing.application.port.in.ManageAssetsUseCase;
import es.terencio.erp.marketing.application.port.out.AssetRepositoryPort;
import es.terencio.erp.marketing.application.port.out.FileStoragePort;
import es.terencio.erp.marketing.application.port.out.FileStoragePort.StorageResult;
import es.terencio.erp.marketing.domain.model.CompanyAsset;
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
        log.info("Uploading new asset {} for company {}", filename, companyId);

        // 1. Upload to S3 (or local storage)
        StorageResult storageResult = fileStoragePort.upload(companyId, filename, contentType, sizeBytes, inputStream,
                isPublic);

        // 2. Save metadata to DB
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
    }

    @Override
    @Transactional(readOnly = true)
    public PageDto<AssetResponse> searchAssets(UUID companyId, String search, String contentType, int page, int size) {
        int offset = page * size;
        long totalElements = assetRepository.countByFilters(companyId, search, contentType);
        List<CompanyAsset> assets = assetRepository.findByFiltersPaginated(companyId, search, contentType, offset,
                size);

        List<AssetResponse> content = assets.stream().map(this::toDto).collect(Collectors.toList());
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return new PageDto<>(content, page, size, totalElements, totalPages);
    }

    @Override
    @Transactional
    public void deleteAsset(UUID companyId, UUID assetId) {
        CompanyAsset asset = assetRepository.findByIdAndCompanyId(assetId, companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found"));

        log.info("Deleting asset {} for company {}", assetId, companyId);

        // 1. Delete from S3/Storage
        fileStoragePort.delete(asset.getStoragePath());

        // 2. Delete from DB
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