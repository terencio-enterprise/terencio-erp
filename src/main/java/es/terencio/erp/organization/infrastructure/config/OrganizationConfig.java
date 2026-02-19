package es.terencio.erp.organization.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.port.in.*;
import es.terencio.erp.organization.application.port.out.*;
import es.terencio.erp.organization.application.service.*;

@Configuration
public class OrganizationConfig {

    @Bean
    public CreateCompanyUseCase createCompanyUseCase(CompanyRepository companyRepository) {
        return new CreateCompanyService(companyRepository);
    }

    @Bean
    public CreateStoreUseCase createStoreUseCase(StoreRepository storeRepository, WarehouseRepository warehouseRepository, StoreSettingsRepository storeSettingsRepository, CompanyRepository companyRepository) {
        return new CreateStoreService(storeRepository, warehouseRepository, storeSettingsRepository, companyRepository);
    }

    @Bean
    public UpdateStoreSettingsUseCase updateStoreSettingsUseCase(StoreSettingsRepository storeSettingsRepository) {
        return new UpdateStoreSettingsService(storeSettingsRepository);
    }

    @Bean
    public DeleteStoreUseCase deleteStoreUseCase(StoreRepository storeRepository) {
        return new DeleteStoreService(storeRepository);
    }

    @Bean
    public UpdateFiscalSettingsUseCase updateFiscalSettingsUseCase(CompanyRepository companyRepository) {
        return new UpdateFiscalSettingsService(companyRepository);
    }

    @Bean
    public OrganizationTreeUseCase organizationTreeUseCase(EmployeePort employeePort, OrganizationRepository organizationRepository) {
        return new OrganizationTreeService(employeePort, organizationRepository);
    }
}
