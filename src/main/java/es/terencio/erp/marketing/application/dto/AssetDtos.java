package es.terencio.erp.marketing.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AssetDtos {
    private AssetDtos() {
    }

    public record AssetResponse(
            UUID id,
            String filename,
            String contentType,
            long fileSizeBytes,
            String publicUrl,
            boolean isPublic,
            Instant createdAt) {
    }

    public record PageDto<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages) {
    }
}