package es.terencio.erp.catalog.application.port.out;

import es.terencio.erp.catalog.domain.model.Category;
import es.terencio.erp.shared.domain.identifier.CompanyId;

import java.util.List;
import java.util.Optional;

/**
 * Output port for Category persistence.
 */
public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(Long id);

    List<Category> findByCompanyId(CompanyId companyId);
}
