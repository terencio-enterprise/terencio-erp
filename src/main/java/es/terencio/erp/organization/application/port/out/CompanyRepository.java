package es.terencio.erp.organization.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.shared.domain.identifier.CompanyId;

public interface CompanyRepository {
    boolean existsByTaxId(String taxId);
    boolean existsById(CompanyId companyId);
    Company save(Company company);
    Optional<Company> findById(CompanyId companyId);
    List<Company> findVisibleCompaniesByEmployeeUuid(UUID employeeUuid);
}