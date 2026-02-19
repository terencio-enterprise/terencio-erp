package es.terencio.erp.marketing.infrastructure.out.storage;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import es.terencio.erp.marketing.application.port.out.StorageSystemPort;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
@Profile("prod")
public class AwsS3StorageAdapter implements StorageSystemPort {
    private final S3Client s3Client;
    private final String defaultBucket = "terencio-marketing-assets";

    public AwsS3StorageAdapter(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String upload(MultipartFile file, String path) {
        try {
            PutObjectRequest putOb = PutObjectRequest.builder().bucket(defaultBucket).key(path).contentType(file.getContentType()).build();
            s3Client.putObject(putOb, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return path;
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public InputStream download(String bucket, String key) {
        return s3Client.getObject(b -> b.bucket(bucket).key(key));
    }

    @Override
    public void delete(String bucket, String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    }
}
