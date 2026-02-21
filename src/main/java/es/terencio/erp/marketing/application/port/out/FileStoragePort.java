package es.terencio.erp.marketing.application.port.out;

import java.io.InputStream;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.asset.StorageResult;

public interface FileStoragePort {
    StorageResult upload(UUID companyId, String filename, String contentType, long sizeBytes, InputStream inputStream,
            boolean isPublic);

    void delete(String storagePath);
}