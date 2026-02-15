package es.terencio.erp.catalog.application.port.out;

import es.terencio.erp.catalog.domain.model.Tax;
import es.terencio.erp.shared.domain.identifier.CompanyId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for Tax persistence.
 */
public interface TaxRepository {

    Tax save(Tax tax);

    Optional<Tax> findById(Long id);

    List<Tax> findByCompanyId(CompanyId companyId);
}
