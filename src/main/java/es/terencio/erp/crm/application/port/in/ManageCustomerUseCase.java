package es.terencio.erp.crm.application.port.in;

import java.util.UUID;

import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;
import es.terencio.erp.shared.domain.query.PageResult;

public interface ManageCustomerUseCase {

    Customer getByUuid(UUID companyId, UUID customerUuid);
    
    PageResult<Customer> search(UUID companyId, CustomerQuery query);
    
    Customer create(UUID companyId, CreateCustomerCommand command);
    
    Customer update(UUID companyId, UUID customerUuid, UpdateCustomerCommand command);
    
    void delete(UUID companyId, UUID customerUuid);

    // --- Commands & Queries ---

    record CustomerQuery(
        String search, 
        CustomerType type, 
        Boolean active, 
        int page, 
        int size
    ) {}

    record CreateCustomerCommand(
        String legalName,
        String taxId,
        CustomerType type,
        String email,
        String phone
    ) {}

    record UpdateCustomerCommand(
        String legalName,
        String commercialName,
        String taxId,
        Customer.ContactInfo contactInfo,
        Customer.BillingInfo billingInfo,
        String notes
    ) {}
}