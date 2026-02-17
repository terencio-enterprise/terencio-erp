package es.terencio.erp.marketing.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingAttachment {
    private Long id;
    private Long templateId;
    private String filename;
    private String contentType;
    private Long fileSizeBytes;
    private String s3Bucket;
    private String s3Key;
    private String s3Region;
}
