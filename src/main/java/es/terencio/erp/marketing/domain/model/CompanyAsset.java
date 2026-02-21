package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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

    public CompanyAsset(UUID companyId, String filename, String contentType, long fileSizeBytes,
            String storagePath, String publicUrl, boolean isPublic) {
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