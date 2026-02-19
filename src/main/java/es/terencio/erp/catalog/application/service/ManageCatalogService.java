package es.terencio.erp.catalog.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.catalog.application.port.in.ListTaxesUseCase;
import es.terencio.erp.catalog.application.port.in.SearchProductsUseCase;
import es.terencio.erp.catalog.application.port.out.ProductRepository;
import es.terencio.erp.catalog.application.port.out.TaxRepository;
import es.terencio.erp.catalog.domain.model.Product;
import es.terencio.erp.catalog.domain.model.Tax;
import es.terencio.erp.shared.domain.identifier.CompanyId;

/**
 * Application service handling catalog query use cases.
 * Provides proper hexagonal boundaries so controllers never touch repositories
 * directly.
 */
@Service
@Transactional(readOnly = true)
public class ManageCatalogService implements SearchProductsUseCase, ListTaxesUseCase {

    private final ProductRepository productRepository;
    private final TaxRepository taxRepository;

    public ManageCatalogService(ProductRepository productRepository, TaxRepository taxRepository) {
        this.productRepository = productRepository;
        this.taxRepository = taxRepository;
    }

    @Override
    public List<Product> search(CompanyId companyId, String name, String reference, Long categoryId, int page,
            int size) {
        return productRepository.search(companyId, name, reference, categoryId, page, size);
    }

    @Override
    public List<Tax> listTaxes(CompanyId companyId) {
        return taxRepository.findByCompanyId(companyId);
    }
}
