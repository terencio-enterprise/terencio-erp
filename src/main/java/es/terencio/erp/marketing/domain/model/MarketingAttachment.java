package es.terencio.erp.marketing.domain.model;

public class MarketingAttachment {

    private Long id;
    private final Long templateId;
    private final String filename;
    private final String contentType;
    private final Long fileSizeBytes;
    private final String s3Bucket;
    private final String s3Key;
    private final String s3Region;

    public MarketingAttachment(
            Long id,
            Long templateId,
            String filename,
            String contentType,
            Long fileSizeBytes,
            String s3Bucket,
            String s3Key,
            String s3Region) {
        this.id = id;
        this.templateId = templateId;
        this.filename = filename;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.s3Bucket = s3Bucket;
        this.s3Key = s3Key;
        this.s3Region = s3Region;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getS3Key() {
        return s3Key;
    }

    public String getS3Region() {
        return s3Region;
    }

    // Infrastructure-only setter
    public void setId(Long id) {
        this.id = id;
    }
}
