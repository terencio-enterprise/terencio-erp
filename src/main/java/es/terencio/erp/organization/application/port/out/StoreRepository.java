package es.terencio.erp.organization.application.port.out;

import java.util.List;
import java.util.Optional;

import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;

/**
 * Output port for Store persistence.
 */
public interface StoreRepository {

    Store save(Store store);

    Optional<Store> findById(StoreId id);

    Optional<Store> findByCode(String code);

    List<Store> findAll();

    List<Store> findByCompanyId(CompanyId companyId);

    boolean existsByCompanyAndCode(CompanyId companyId, String code);

    boolean hasDependencies(StoreId id);

    void delete(StoreId id);
}
