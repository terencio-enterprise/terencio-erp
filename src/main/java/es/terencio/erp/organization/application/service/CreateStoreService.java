package es.terencio.erp.organization.application.service;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateStoreResult;
import es.terencio.erp.organization.application.port.in.CreateStoreUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.application.port.out.WarehouseRepository;
import es.terencio.erp.organization.domain.model.Address;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.organization.domain.model.Warehouse;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.TaxId;
import es.terencio.erp.shared.exception.DomainException;

public class CreateStoreService implements CreateStoreUseCase {
    private final StoreRepository storeRepository;
    private final WarehouseRepository warehouseRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final CompanyRepository companyRepository;

    public CreateStoreService(StoreRepository storeRepository, WarehouseRepository warehouseRepository,
            StoreSettingsRepository storeSettingsRepository, CompanyRepository companyRepository) {
        this.storeRepository = storeRepository;
        this.warehouseRepository = warehouseRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.companyRepository = companyRepository;
    }

    @Override
    public CreateStoreResult execute(CreateStoreCommand command) {
        CompanyId companyId = new CompanyId(command.companyId());
        companyRepository.findById(companyId).orElseThrow(() -> new DomainException("Company not found"));

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
}
