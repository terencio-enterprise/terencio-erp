package es.terencio.erp.catalog.application.port.out;

import java.util.Optional;

import es.terencio.erp.catalog.domain.model.ProductPrice;
import es.terencio.erp.shared.domain.identifier.ProductId;

/**
 * Output port for ProductPrice persistence.
 */
public interface ProductPriceRepository {

    ProductPrice save(ProductPrice productPrice);

    Optional<ProductPrice> findByProductAndTariff(ProductId productId, Long tariffId);
}
