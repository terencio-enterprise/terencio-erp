package es.terencio.erp.organization.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreResult;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateStoreSettingsCommand;
import es.terencio.erp.organization.application.port.in.StoreUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.application.port.out.WarehouseRepository;
import es.terencio.erp.organization.domain.model.Address;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.organization.domain.model.Warehouse;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.domain.valueobject.TaxId;
import es.terencio.erp.shared.exception.DomainException;

public class StoreService implements StoreUseCase {

    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final CompanyRepository companyRepository;

    public StoreService(StoreRepository storeRepository, WarehouseRepository warehouseRepository,
                        StoreSettingsRepository storeSettingsRepository, CompanyRepository companyRepository) {
        this.storeRepository = storeRepository;
        this.warehouseRepository = warehouseRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional
    public CreateStoreResult create(CreateStoreCommand command) {
        CompanyId companyId = new CompanyId(command.companyId());
        if (!companyRepository.existsById(companyId)) {
            throw new DomainException("Company not found");
        }
        if (storeRepository.existsByCompanyAndCode(companyId, command.code())) {
            throw new DomainException("Store code already exists: " + command.code());
        }

        Address address = new Address(command.street(), command.zipCode(), command.city(), "ES");
        TaxId taxId = command.taxId() != null ? TaxId.of(command.taxId()) : null;

        Store store = Store.create(companyId, command.code(), command.name(), address, taxId);
        Store savedStore = storeRepository.save(store);

        Warehouse warehouse = Warehouse.create(savedStore.id(), "Almacén " + command.name(), "WH-" + command.code());
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        StoreSettings settings = StoreSettings.createDefault(savedStore.id());
        storeSettingsRepository.save(settings);

        return new CreateStoreResult(savedStore.id().value(), savedWarehouse.id().value(), savedStore.code(), savedStore.name());
    }

    @Override
    @Transactional(readOnly = true)
    public Store getById(UUID storeId) {
        return storeRepository.findById(new StoreId(storeId))
                .orElseThrow(() -> new DomainException("Store not found: " + storeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Store> getAllByCompany(UUID companyId) {
        return storeRepository.findByCompanyId(new CompanyId(companyId));
    }

    @Override
    @Transactional(readOnly = true)
    public StoreSettings getSettings(UUID storeId) {
        return storeSettingsRepository.findByStoreId(new StoreId(storeId))
                .orElseThrow(() -> new DomainException("Store settings not found"));
    }

    @Override
    @Transactional
    public void updateSettings(UpdateStoreSettingsCommand command) {
        StoreSettings settings = getSettings(command.storeId());
        Money requireCustomerAmount = command.requireCustomerForLargeAmount() != null 
                ? Money.ofEuros(command.requireCustomerForLargeAmount()) 
                : Money.ofEuros(1000.0);

        settings.updateSettings(command.allowNegativeStock(), command.defaultTariffId(), command.printTicketAutomatically(), requireCustomerAmount);
        storeSettingsRepository.save(settings);
    }

    @Override
    @Transactional
    public void delete(UUID storeId) {
        StoreId id = new StoreId(storeId);
        if (!storeRepository.existsById(id)) throw new DomainException("Store not found");
        
        if (storeRepository.hasDependencies(id)) {
            throw new DomainException("Cannot delete Store: It has active personnel or devices.");
        }
        storeRepository.delete(id);
    }
}