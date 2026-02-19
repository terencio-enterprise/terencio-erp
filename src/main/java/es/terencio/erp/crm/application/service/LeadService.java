package es.terencio.erp.crm.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import es.terencio.erp.crm.application.port.out.CustomerRepository;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerType;
import es.terencio.erp.crm.domain.model.MarketingStatus;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService implements IngestLeadUseCase {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void ingest(UUID companyId, LeadCommand command) {
        Customer lead = Customer.create(new CompanyId(companyId),
                command.getCompanyName() != null ? command.getCompanyName() : command.getName(), null);

        lead.updateContactInfo(Email.of(command.getEmail()), command.getPhone(), null, null, null);
        lead.setType(CustomerType.LEAD);
        lead.setOrigin(command.getOrigin());
        lead.setTags(command.getTags());
        lead.setMarketingConsent(command.isConsent());
        lead.setMarketingStatus(command.isConsent() ? MarketingStatus.SUBSCRIBED : MarketingStatus.UNSUBSCRIBED);
        lead.setUnsubscribeToken(UUID.randomUUID().toString());

        customerRepository.save(lead);
        log.info("Ingested lead: {} for company: {}", command.getEmail(), companyId);
    }
}