package es.terencio.erp.sales.application.port.out;

import java.util.Optional;

import es.terencio.erp.sales.domain.model.Sale;
import es.terencio.erp.shared.domain.identifier.SaleId;

/**
 * Output port for Sale persistence.
 */
public interface SaleRepository {

    Sale save(Sale sale);

    Optional<Sale> findByUuid(SaleId uuid);
}
