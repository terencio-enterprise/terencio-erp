package es.terencio.erp.marketing.application.port.out;

import java.io.InputStream;
import java.util.UUID;

public interface FileStoragePort {
    StorageResult upload(UUID companyId, String filename, String contentType, long sizeBytes, InputStream inputStream,
            boolean isPublic);

    void delete(String storagePath);

    record StorageResult(String storagePath, String publicUrl) {
    }
}