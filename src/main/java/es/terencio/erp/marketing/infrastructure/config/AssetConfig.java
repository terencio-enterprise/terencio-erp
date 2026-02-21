package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.terencio.erp.marketing.application.port.in.ManageAssetsUseCase;
import es.terencio.erp.marketing.application.port.out.AssetRepositoryPort;
import es.terencio.erp.marketing.application.port.out.FileStoragePort;
import es.terencio.erp.marketing.application.service.AssetService;

@Configuration
public class AssetConfig {

    @Bean
    public ManageAssetsUseCase manageAssetsUseCase(AssetRepositoryPort assetRepository,
            FileStoragePort fileStoragePort) {
        return new AssetService(assetRepository, fileStoragePort);
    }
}