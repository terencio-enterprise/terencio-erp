package es.terencio.erp.organization.application.port.out;

import java.util.List;
import java.util.Optional;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;

public interface StoreRepository {
    Store save(Store store);
    Optional<Store> findById(StoreId id);
    List<Store> findByCompanyId(CompanyId companyId);
    boolean existsByCompanyAndCode(CompanyId companyId, String code);
    Optional<Store> findByCode(String code);
    List<Store> findAll();
    boolean hasDependencies(StoreId id);
    void delete(StoreId id);
}
