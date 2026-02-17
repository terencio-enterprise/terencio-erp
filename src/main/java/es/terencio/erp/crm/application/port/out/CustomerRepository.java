package es.terencio.erp.crm.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;

/**
 * Output port for Customer persistence.
 */
public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findById(CustomerId id);

    Optional<Customer> findByUuid(UUID uuid);

    List<Customer> findByCompanyId(CompanyId companyId);

    List<Customer> searchCustomers(CompanyId companyId, String searchTerm);

    // Marketing Filters
    List<Customer> findByMarketingCriteria(List<String> tags, String customerType, java.math.BigDecimal minSpent);

    Optional<Customer> findByUnsubscribeToken(String token);
}
