package es.terencio.erp.crm.application.port.in;

import java.util.UUID;

import es.terencio.erp.crm.application.port.in.command.CreateCustomerCommand;
import es.terencio.erp.crm.application.port.in.command.UpdateCustomerCommand;
import es.terencio.erp.crm.application.port.in.query.SearchCustomerQuery;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.query.PageResult;

public interface ManageCustomerUseCase {

    Customer getByUuid(UUID companyId, UUID customerUuid);
    
    PageResult<Customer> search(UUID companyId, SearchCustomerQuery query);
    
    Customer create(UUID companyId, CreateCustomerCommand command);
    
    Customer update(UUID companyId, UUID customerUuid, UpdateCustomerCommand command);
    
    void delete(UUID companyId, UUID customerUuid);
}