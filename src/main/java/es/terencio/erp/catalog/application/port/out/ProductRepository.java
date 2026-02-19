package es.terencio.erp.catalog.application.port.out;

import java.util.List;
import java.util.Optional;

import es.terencio.erp.catalog.domain.model.Product;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;

/**
 * Output port for Product persistence.
 */
public interface ProductRepository {

    Product save(Product product);

    Optional<Product> findById(ProductId id);

    Optional<Product> findByCompanyAndReference(CompanyId companyId, String reference);

    boolean existsByCompanyAndReference(CompanyId companyId, String reference);

    List<Product> search(CompanyId companyId, String name, String reference, Long categoryId, int page, int size);
}
