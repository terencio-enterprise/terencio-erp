package es.terencio.erp.organization.application.port.out;

import java.util.List;
import java.util.Optional;

import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;

public interface StoreRepository {
    boolean existsByCompanyAndCode(CompanyId companyId, String code);
    boolean existsById(StoreId storeId);
    boolean hasDependencies(StoreId storeId);
    Store save(Store store);
    Optional<Store> findById(StoreId storeId);
    List<Store> findByCompanyId(CompanyId companyId);
    void delete(StoreId storeId);
}