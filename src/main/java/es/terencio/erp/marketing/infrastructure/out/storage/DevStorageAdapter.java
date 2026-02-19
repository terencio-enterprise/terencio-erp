package es.terencio.erp.marketing.infrastructure.out.storage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import es.terencio.erp.marketing.application.port.out.StorageSystemPort;

@Component
@Profile("!prod")
public class DevStorageAdapter implements StorageSystemPort {
    private static final Logger log = LoggerFactory.getLogger(DevStorageAdapter.class);

    @Override
    public String upload(MultipartFile file, String path) {
        log.info("========== [DEV MODE] Uploading File ==========");
        log.info("Filename: {}", file.getOriginalFilename());
        log.info("Path: {}", path);
        return path;
    }

    @Override
    public InputStream download(String bucket, String key) {
        log.info("========== [DEV MODE] Downloading File ==========");
        return new ByteArrayInputStream("DEV_CONTENT".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void delete(String bucket, String key) {
        log.info("========== [DEV MODE] Deleting File ==========");
    }
}
