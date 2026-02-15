package es.terencio.erp.organization.application.port.out;

import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.shared.domain.identifier.CompanyId;

import java.util.Optional;

/**
 * Output port for Company persistence.
 */
public interface CompanyRepository {

    Company save(Company company);

    Optional<Company> findById(CompanyId id);

    boolean existsByTaxId(String taxId);
}
