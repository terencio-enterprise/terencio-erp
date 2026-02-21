package es.terencio.erp.crm.application.port.out;

import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase.CustomerQuery;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.presentation.PageResult;

public interface CustomerRepositoryPort {

    Customer save(Customer customer);

    Optional<Customer> findByUuidAndCompanyId(UUID uuid, CompanyId companyId);

    boolean existsByEmailAndCompanyId(Email email, CompanyId companyId);

    PageResult<Customer> searchPaginated(CompanyId companyId, CustomerQuery query);
}