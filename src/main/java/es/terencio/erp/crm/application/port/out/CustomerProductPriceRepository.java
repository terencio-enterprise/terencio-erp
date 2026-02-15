package es.terencio.erp.crm.application.port.out;

import java.util.List;
import java.util.Optional;

import es.terencio.erp.crm.domain.model.CustomerProductPrice;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.identifier.ProductId;

/**
 * Output port for CustomerProductPrice persistence.
 */
public interface CustomerProductPriceRepository {

    void save(CustomerProductPrice price);

    Optional<CustomerProductPrice> findByCustomerAndProduct(CustomerId customerId, ProductId productId);

    List<CustomerProductPrice> findByCustomerId(CustomerId customerId);
}
