package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import lombok.Getter;

@Getter
public class CompanyAsset {
    private UUID id;
    private UUID companyId;
    private String filename;
    private String contentType;
    private long fileSizeBytes;
    private String storagePath;
    private String publicUrl;
    private boolean isPublic;
    private Instant createdAt;
    private Instant updatedAt;

    protected CompanyAsset() {
    }

    public CompanyAsset(UUID companyId, String filename, String contentType, long fileSizeBytes,
            String storagePath, String publicUrl, boolean isPublic) {
        if (companyId == null || filename == null || filename.isBlank() || storagePath == null || storagePath.isBlank()) {
            throw new InvariantViolationException("CompanyAsset requires company, filename, and storage path");
        }
        
        this.id = UUID.randomUUID();
        this.companyId = companyId;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.storagePath = storagePath;
        this.publicUrl = publicUrl;
        this.isPublic = isPublic;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}