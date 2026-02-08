package es.terencio.erp.stores.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.stores.application.dto.StoreDto;

public interface StorePort {
    List<StoreDto> findAll();
    Optional<StoreDto> findById(UUID id);
    Optional<StoreDto> findByCode(String code);
    void save(StoreDto store);
    void update(StoreDto store);
    boolean hasDependencies(UUID id);
}