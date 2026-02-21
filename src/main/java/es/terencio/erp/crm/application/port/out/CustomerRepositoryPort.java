package es.terencio.erp.crm.application.port.out;

import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.crm.application.port.in.query.SearchCustomerQuery;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.domain.valueobject.Email;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findByUuidAndCompanyId(UUID uuid, CompanyId companyId);

    boolean existsByEmailAndCompanyId(Email email, CompanyId companyId);

    PageResult<Customer> searchPaginated(CompanyId companyId, SearchCustomerQuery query);
}