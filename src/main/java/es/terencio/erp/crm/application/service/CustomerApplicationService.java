package es.terencio.erp.crm.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import es.terencio.erp.crm.application.port.in.ManageCustomerUseCase;
import es.terencio.erp.crm.application.port.in.command.CreateCustomerCommand;
import es.terencio.erp.crm.application.port.in.command.IngestLeadCommand;
import es.terencio.erp.crm.application.port.in.command.UpdateCustomerCommand;
import es.terencio.erp.crm.application.port.in.query.SearchCustomerQuery;
import es.terencio.erp.crm.application.port.out.CustomerRepositoryPort;
import es.terencio.erp.crm.domain.model.BillingInfo;
import es.terencio.erp.crm.domain.model.ContactInfo;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.TaxId;
import es.terencio.erp.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerApplicationService implements IngestLeadUseCase, ManageCustomerUseCase {

    private final CustomerRepositoryPort customerRepository;

    @Override
    @Transactional
    public void ingest(UUID companyId, IngestLeadCommand command) {
        CompanyId cid = new CompanyId(companyId);
        Email email = Email.of(command.email());

        if (customerRepository.existsByEmailAndCompanyId(email, cid)) {
            log.info("Lead already exists: {}", email);
            return;
        }

        String name = (command.companyName() != null && !command.companyName().isBlank())
                ? command.companyName()
                : command.name();

        Customer lead = Customer.newLead(
                cid,
                name,
                email,
                command.phone(),
                command.origin(),
                command.tags(),
                command.consent()
        );

        customerRepository.save(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getByUuid(UUID companyId, UUID customerUuid) {
        return customerRepository.findByUuidAndCompanyId(customerUuid, new CompanyId(companyId))
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Customer> search(UUID companyId, SearchCustomerQuery query) {

        int safeSize = Math.min(Math.max(query.size(), 1), 100);
        int safePage = Math.max(query.page(), 0);

        SearchCustomerQuery safeQuery = new SearchCustomerQuery(
                query.search(),
                query.type(),
                query.active(),
                safePage,
                safeSize
        );

        return customerRepository.searchPaginated(new CompanyId(companyId), safeQuery);
    }

    @Override
    @Transactional
    public Customer create(UUID companyId, CreateCustomerCommand command) {

        CompanyId cid = new CompanyId(companyId);
        TaxId taxId = command.taxId() != null ? TaxId.of(command.taxId()) : null;

        Customer customer = Customer.newClient(cid, command.legalName(), taxId, command.type());

        if (command.email() != null || command.phone() != null) {
            customer.updateContactInfo(new ContactInfo(
                    command.email() != null ? Email.of(command.email()) : null,
                    command.phone(),
                    null,
                    null,
                    null,
                    "ES"
            ));
        }

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public Customer update(UUID companyId, UUID customerUuid, UpdateCustomerCommand command) {

        Customer customer = getByUuid(companyId, customerUuid);

        customer.rename(command.legalName(), command.commercialName());

        if (command.taxId() != null) {
            customer.changeTaxId(TaxId.of(command.taxId()));
        }

        customer.updateNotes(command.notes());

        if (command.contactInfo() != null) {
            customer.updateContactInfo(new ContactInfo(
                    command.contactInfo().email() != null ? Email.of(command.contactInfo().email()) : null,
                    command.contactInfo().phone(),
                    command.contactInfo().address(),
                    command.contactInfo().zipCode(),
                    command.contactInfo().city(),
                    command.contactInfo().country() != null ? command.contactInfo().country() : "ES"
            ));
        }

        if (command.billingInfo() != null) {
            BillingInfo base = customer.getBillingInfo() != null
                    ? customer.getBillingInfo()
                    : BillingInfo.defaultSettings();

            customer.updateBillingInfo(new BillingInfo(
                    command.billingInfo().tariffId() != null ? command.billingInfo().tariffId() : base.tariffId(),
                    command.billingInfo().allowCredit() != null ? command.billingInfo().allowCredit() : base.allowCredit(),
                    command.billingInfo().creditLimitCents() != null ? command.billingInfo().creditLimitCents() : base.creditLimitCents(),
                    command.billingInfo().surchargeApply() != null ? command.billingInfo().surchargeApply() : base.surchargeApply()
            ));
        }

        return customerRepository.save(customer);
    }

    @Override
    @Transactional
    public void delete(UUID companyId, UUID customerUuid) {
        Customer customer = getByUuid(companyId, customerUuid);
        customer.deactivate();
        customerRepository.save(customer);
    }
}