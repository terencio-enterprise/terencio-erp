package es.terencio.erp.organization.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.port.in.CompanyUseCase;
import es.terencio.erp.organization.application.port.in.OrganizationUseCase;
import es.terencio.erp.organization.application.port.in.StoreUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.application.port.out.OrganizationRepository;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.application.port.out.WarehouseRepository;
import es.terencio.erp.organization.application.service.CompanyService;
import es.terencio.erp.organization.application.service.OrganizationService;
import es.terencio.erp.organization.application.service.StoreService;

@Configuration
public class OrganizationConfig {

    @Bean
    public CompanyUseCase companyUseCase(CompanyRepository companyRepository) {
        return new CompanyService(companyRepository);
    }

    @Bean
    public StoreUseCase storeUseCase(StoreRepository storeRepository, WarehouseRepository warehouseRepository, 
                                     StoreSettingsRepository storeSettingsRepository, CompanyRepository companyRepository) {
        return new StoreService(storeRepository, warehouseRepository, storeSettingsRepository, companyRepository);
    }

    @Bean
    public OrganizationUseCase organizationUseCase(EmployeePort employeePort, OrganizationRepository organizationRepository) {
        return new OrganizationService(employeePort, organizationRepository);
    }
}