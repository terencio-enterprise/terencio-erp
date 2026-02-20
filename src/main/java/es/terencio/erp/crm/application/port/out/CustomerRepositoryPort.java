package es.terencio.erp.crm.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.identifier.CompanyId;

public interface CustomerRepositoryPort {
    Customer save(Customer customer);
    Optional<Customer> findByUuid(UUID uuid);
    List<Customer> findByCompanyId(CompanyId companyId);
    List<Customer> searchCustomers(CompanyId companyId, String search);
}