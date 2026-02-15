package es.terencio.erp.organization.application.port.out;

import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.StoreId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for Store persistence.
 */
public interface StoreRepository {

    Store save(Store store);

    Optional<Store> findById(StoreId id);

    List<Store> findByCompanyId(CompanyId companyId);

    boolean existsByCompanyAndCode(CompanyId companyId, String code);
}
