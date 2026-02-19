package es.terencio.erp.organization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.terencio.erp.organization.application.port.in.CreateCompanyUseCase;
import es.terencio.erp.organization.application.port.in.CreateStoreUseCase;
import es.terencio.erp.organization.application.port.in.DeleteStoreUseCase;
import es.terencio.erp.organization.application.port.in.UpdateStoreSettingsUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.application.port.out.WarehouseRepository;
import es.terencio.erp.organization.application.usecase.CreateCompanyService;
import es.terencio.erp.organization.application.usecase.CreateStoreService;
import es.terencio.erp.organization.application.usecase.DeleteStoreService;
import es.terencio.erp.organization.application.usecase.UpdateStoreSettingsService;

/**
 * Spring configuration for Organization module use cases.
 */
@Configuration
public class OrganizationConfig {

    @Bean
    public CreateCompanyUseCase createCompanyUseCase(CompanyRepository companyRepository) {
        return new CreateCompanyService(companyRepository);
    }

    @Bean
    public CreateStoreUseCase createStoreUseCase(
            StoreRepository storeRepository,
            WarehouseRepository warehouseRepository,
            StoreSettingsRepository storeSettingsRepository,
            CompanyRepository companyRepository) {
        return new CreateStoreService(storeRepository, warehouseRepository, storeSettingsRepository, companyRepository);
    }

    @Bean
    public UpdateStoreSettingsUseCase updateStoreSettingsUseCase(
            StoreSettingsRepository storeSettingsRepository) {
        return new UpdateStoreSettingsService(storeSettingsRepository);
    }

    @Bean
    public DeleteStoreUseCase deleteStoreUseCase(StoreRepository storeRepository) {
        return new DeleteStoreService(storeRepository);
    }
}
