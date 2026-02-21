package es.terencio.erp.marketing.infrastructure.out.storage;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import es.terencio.erp.marketing.application.port.out.FileStoragePort;

@Component
@Profile("!prod")
public class LocalFileStorageAdapter implements FileStoragePort {

    private final Path rootLocation;
    private final String publicBaseUrl;

    public LocalFileStorageAdapter(@Value("${app.marketing.assets.local-dir:./uploads}") String localDir,
            @Value("${app.marketing.public-base-url:http://localhost:8080}") String publicBaseUrl) {
        this.rootLocation = Paths.get(localDir);
        this.publicBaseUrl = publicBaseUrl;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize local storage directory");
        }
    }

    @Override
    public StorageResult upload(UUID companyId, String filename, String contentType, long sizeBytes,
            InputStream inputStream, boolean isPublic) {
        try {
            String safeFilename = filename.replaceAll("[^a-zA-Z0-9.-]", "_");
            String storagePath = companyId.toString() + "/" + UUID.randomUUID().toString() + "_" + safeFilename;
            Path destinationFile = this.rootLocation.resolve(Paths.get(storagePath)).normalize().toAbsolutePath();

            Files.createDirectories(destinationFile.getParent());
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);

            String url = isPublic ? publicBaseUrl + "/assets/" + storagePath : null;
            return new StorageResult(storagePath, url);
        } catch (Exception e) {
            throw new RuntimeException("Failed to store file locally.", e);
        }
    }

    @Override
    public void delete(String storagePath) {
        try {
            Path file = this.rootLocation.resolve(storagePath);
            Files.deleteIfExists(file);
        } catch (Exception e) {
            System.err.println("Failed to delete local asset: " + storagePath);
        }
    }
}