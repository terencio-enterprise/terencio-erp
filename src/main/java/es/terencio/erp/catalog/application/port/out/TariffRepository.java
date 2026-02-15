package es.terencio.erp.catalog.application.port.out;

import es.terencio.erp.catalog.domain.model.Tariff;
import es.terencio.erp.shared.domain.identifier.CompanyId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for Tariff persistence.
 */
public interface TariffRepository {

    Tariff save(Tariff tariff);

    Optional<Tariff> findById(Long id);

    List<Tariff> findByCompanyId(CompanyId companyId);
}
