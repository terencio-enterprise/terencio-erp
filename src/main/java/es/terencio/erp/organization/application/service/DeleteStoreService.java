package es.terencio.erp.organization.application.service;

import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import es.terencio.erp.organization.application.port.in.DeleteStoreUseCase;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.exception.DomainException;

public class DeleteStoreService implements DeleteStoreUseCase {
    private final StoreRepository storeRepository;

    public DeleteStoreService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    @Override
    @Transactional
    public void execute(UUID storeId) {
        StoreId id = new StoreId(storeId);
        storeRepository.findById(id).orElseThrow(() -> new DomainException("Store not found: " + storeId));
        if (storeRepository.hasDependencies(id)) {
            throw new DomainException("Cannot delete Store: It has active personnel or non-inactive devices.");
        }
        storeRepository.delete(id);
    }
}
