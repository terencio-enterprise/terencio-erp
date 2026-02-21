package es.terencio.erp.marketing.application.dto.asset;

import java.time.Instant;
import java.util.UUID;

public record AssetResponse(
        UUID id,
        String filename,
        String contentType,
        long fileSizeBytes,
        String publicUrl,
        boolean isPublic,
        Instant createdAt
) {}