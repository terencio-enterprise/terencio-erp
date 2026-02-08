package es.terencio.erp.stores.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.shared.exception.DomainException;
import es.terencio.erp.stores.application.dto.StoreDto;
import es.terencio.erp.stores.application.port.in.ManageStoresUseCase;
import es.terencio.erp.stores.application.port.out.StorePort;

@Service
public class StoreService implements ManageStoresUseCase {

    private final StorePort storePort;

    public StoreService(StorePort storePort) {
        this.storePort = storePort;
    }

    @Override
    public List<StoreDto> listAll() {
        return storePort.findAll();
    }

    @Override
    public StoreDto getById(UUID id) {
        return storePort.findById(id)
                .orElseThrow(() -> new DomainException("Store not found"));
    }

    @Override
    @Transactional
    public StoreDto create(StoreDto request) {
        if (storePort.findByCode(request.code()).isPresent()) {
            throw new DomainException("Store code already exists: " + request.code());
        }

        StoreDto newStore = new StoreDto(
                UUID.randomUUID(),
                request.code(),
                request.name(),
                request.address(),
                request.taxId(),
                true
        );

        storePort.save(newStore);
        return newStore;
    }

    @Override
    @Transactional
    public StoreDto update(UUID id, StoreDto request) {
        StoreDto existing = getById(id);

        // Check code uniqueness if changed
        if (!existing.code().equals(request.code()) && storePort.findByCode(request.code()).isPresent()) {
            throw new DomainException("Store code already exists: " + request.code());
        }

        StoreDto updated = new StoreDto(
                id,
                request.code(),
                request.name(),
                request.address(),
                request.taxId(),
                request.isActive()
        );

        storePort.update(updated);
        return updated;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        StoreDto existing = getById(id);
        
        // Logical check: Don't allow deleting/disabling a store if it has active entities
        if (existing.isActive() && storePort.hasDependencies(id)) {
            throw new DomainException("Cannot delete Store: It has active Users or POS Devices associated with it. Please reassign or remove them first.");
        }

        StoreDto deleted = new StoreDto(
                id,
                existing.code(),
                existing.name(),
                existing.address(),
                existing.taxId(),
                false // Soft delete
        );
        storePort.update(deleted);
    }
}