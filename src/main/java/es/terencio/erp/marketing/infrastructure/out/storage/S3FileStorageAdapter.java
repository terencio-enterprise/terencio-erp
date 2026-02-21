package es.terencio.erp.marketing.infrastructure.out.storage;

import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import es.terencio.erp.marketing.application.port.out.FileStoragePort;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@Profile("prod")
public class S3FileStorageAdapter implements FileStoragePort {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicBaseUrl;

    public S3FileStorageAdapter(S3Client s3Client,
            @Value("${app.marketing.assets.s3-bucket:marketing-assets}") String bucketName,
            @Value("${app.marketing.assets.public-base-url:https://assets.terencio.es}") String publicBaseUrl) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Override
    public StorageResult upload(UUID companyId, String filename, String contentType, long sizeBytes,
            InputStream inputStream, boolean isPublic) {
        String safeFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
        String storagePath = companyId.toString() + "/" + UUID.randomUUID().toString() + "_" + safeFilename;

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storagePath)
                .contentType(contentType);

        if (isPublic) {
            requestBuilder.acl(ObjectCannedACL.PUBLIC_READ);
        }

        try {
            s3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(inputStream, sizeBytes));
            String url = isPublic ? publicBaseUrl + "/" + storagePath : null;
            return new StorageResult(storagePath, url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload asset to S3: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(storagePath)
                    .build());
        } catch (Exception e) {
            System.err.println("Failed to delete asset from S3: " + storagePath);
        }
    }
}