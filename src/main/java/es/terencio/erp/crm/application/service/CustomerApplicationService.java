package es.terencio.erp.crm.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase;
import es.terencio.erp.crm.application.port.out.CustomerRepositoryPort;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.TaxId;
import es.terencio.erp.shared.exception.ResourceNotFoundException;
import es.terencio.erp.shared.presentation.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerApplicationService implements IngestLeadUseCase, ManageCustomerUseCase {

    private final CustomerRepositoryPort customerRepository;

    @Override
    @Transactional
    public void ingest(UUID companyId, LeadCommand command) {
        CompanyId cid = new CompanyId(companyId);
        Email email = Email.of(command.email());

        if (customerRepository.existsByEmailAndCompanyId(email, cid)) {
            log.info("Lead {} already exists for company {}", email, companyId);
            return;
        }

        String targetName = (command.companyName() != null && !command.companyName().isBlank())
                ? command.companyName()
                : command.name();

        Customer lead = Customer.newLead(cid, targetName, email, command.phone(), command.origin(), command.tags(),
                command.consent());

        customerRepository.save(lead);
        log.info("Lead ingested successfully: {} (UUID: {})", email, lead.getUuid());
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getByUuid(UUID companyId, UUID customerUuid) {
        return customerRepository.findByUuidAndCompanyId(customerUuid, new CompanyId(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Customer> search(UUID companyId, CustomerQuery query) {
        return customerRepository.searchPaginated(new CompanyId(companyId), query);
    }

    @Override
    @Transactional
    public Customer create(UUID companyId, CreateCustomerCommand command) {
        CompanyId cid = new CompanyId(companyId);
        TaxId taxId = command.taxId() != null ? TaxId.of(command.taxId()) : null;

        Customer newCustomer = Customer.newClient(cid, command.legalName(), taxId, command.type());

        if (command.email() != null || command.phone() != null) {
            newCustomer.updateContactInfo(new Customer.ContactInfo(
                    command.email() != null ? Email.of(command.email()) : null,
                    command.phone(), null, null, null, "ES"));
        }

        return customerRepository.save(newCustomer);
    }

    @Override
    @Transactional
    public Customer update(UUID companyId, UUID customerUuid, UpdateCustomerCommand command) {
        Customer customer = getByUuid(companyId, customerUuid);

        TaxId taxId = command.taxId() != null ? TaxId.of(command.taxId()) : null;
        customer.updateDetails(command.legalName(), command.commercialName(), taxId, command.notes());

        if (command.contactInfo() != null)
            customer.updateContactInfo(command.contactInfo());
        if (command.billingInfo() != null)
            customer.updateBillingInfo(command.billingInfo());

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID customerUuid) {
        Customer customer = getByUuid(companyId, customerUuid);
        customer.markAsDeleted();
        customerRepository.save(customer);
        log.info("Customer {} marked as deleted", customerUuid);
    }
}