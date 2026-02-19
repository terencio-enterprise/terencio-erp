package es.terencio.erp.catalog.application.port.in;

import java.util.List;

import es.terencio.erp.catalog.domain.model.Product;
import es.terencio.erp.shared.domain.identifier.CompanyId;

/**
 * Input port for product search operations.
 */
public interface SearchProductsUseCase {

    List<Product> search(CompanyId companyId, String name, String reference, Long categoryId, int page, int size);
}
