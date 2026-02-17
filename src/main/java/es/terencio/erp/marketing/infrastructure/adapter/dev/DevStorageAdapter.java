package es.terencio.erp.marketing.infrastructure.adapter.dev;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import es.terencio.erp.marketing.application.port.out.StorageSystemPort;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!prod")
@Slf4j
public class DevStorageAdapter implements StorageSystemPort {

    @Override
    public String upload(MultipartFile file, String path) {
        log.info("========== [DEV MODE] Uploading File ==========");
        log.info("Filename: {}", file.getOriginalFilename());
        log.info("Path: {}", path);
        log.info("Size: {}", file.getSize());
        log.info("===============================================");
        return path;
    }

    @Override
    public InputStream download(String bucket, String key) {
        log.info("========== [DEV MODE] Downloading File ==========");
        log.info("Bucket: {}", bucket);
        log.info("Key: {}", key);
        log.info("===============================================");
        return new ByteArrayInputStream("DEV_CONTENT".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void delete(String bucket, String key) {
        log.info("========== [DEV MODE] Deleting File ==========");
        log.info("Bucket: {}", bucket);
        log.info("Key: {}", key);
        log.info("=============================================");
    }
}
