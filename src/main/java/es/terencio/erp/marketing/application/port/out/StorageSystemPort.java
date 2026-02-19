package es.terencio.erp.marketing.application.port.out;

import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;

public interface StorageSystemPort {
    String upload(MultipartFile file, String path);
    InputStream download(String bucket, String key);
    void delete(String bucket, String key);
}
