package es.terencio.erp.stores.application.port.in;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.stores.application.dto.StoreDto;

public interface ManageStoresUseCase {
    List<StoreDto> listAll();
    StoreDto getById(UUID id);
    StoreDto create(StoreDto request);
    StoreDto update(UUID id, StoreDto request);
    void delete(UUID id);
}