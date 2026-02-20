package es.terencio.erp.crm.application.port.out;

import java.util.List;
import es.terencio.erp.crm.domain.model.CustomerProductPrice;
import es.terencio.erp.shared.domain.identifier.CustomerId;

public interface CustomerProductPriceRepositoryPort {
    CustomerProductPrice save(CustomerProductPrice price);
    List<CustomerProductPrice> findByCustomerId(CustomerId customerId);
}