package es.terencio.erp.crm.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import es.terencio.erp.crm.application.port.out.CustomerRepositoryPort;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.MarketingStatus;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.utils.SecurityUtils;
import es.terencio.erp.shared.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService implements IngestLeadUseCase {

    private final CustomerRepositoryPort customerRepository;

    @Override
    @Transactional
    public void ingest(UUID companyId, LeadCommand command) {
        CompanyId cid = new CompanyId(companyId);
        Email email = Email.of(command.email());

        // 1. Check if lead already exists to avoid duplicates
        boolean exists = customerRepository.findByCompanyId(cid)
                .stream()
                .anyMatch(c -> c.email() != null && c.email().equals(email));

        if (exists) {
            log.info("Lead ingestion skipped: {} already exists for company {}", command.email(), companyId);
            return;
        }

        // 2. Map command to Domain Model
        String displayName = (command.companyName() != null && !command.companyName().isBlank())
                ? command.companyName()
                : command.name();

        Customer lead = Customer.createLead(cid, displayName, email);

        // 3. Enrich lead data
        lead.updateContactInfo(email, command.phone(), null, null, null);
        lead.setOrigin(command.origin());
        lead.setTags(command.tags());
        lead.setMarketingConsent(command.consent());
        lead.setMarketingStatus(command.consent() ? MarketingStatus.SUBSCRIBED : MarketingStatus.UNSUBSCRIBED);
        lead.setUnsubscribeToken(SecurityUtils.generateSecureToken());

        // 4. Persistence
        customerRepository.save(lead);

        log.info("Lead successfully ingested: {} (ID: {})", command.email(), lead.uuid());
    }
}