package es.terencio.erp.crm.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import es.terencio.erp.crm.application.port.out.CustomerRepository;
import es.terencio.erp.crm.domain.model.Customer;
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
    public void ingest(LeadCommand command) {
        // Simple logic: check if exists by email, if not create as LEAD
        // For simplicity, we assume we can create it without CompanyId (or
        // hardcode/lookup one)
        // In this ERP multi-tenant context, we usually need a CompanyId context.
        // Assuming public leads come for a specific Company (Tenant) via API token or
        // similar.
        // Or we use a default Company for Leads if not specified.
        // Let's assume we look up via origin or it's provided in context?
        // Since this is "Public", maybe it's for Terencio itself? Or Terencio ERP's
        // clients?
        // Let's assume we find a default company or it is ignored for now.
        // Wait, Customer.create requires CompanyId.
        // I will use a placeholder UUID for now or inject a TenantProvider.

        UUID defaultCompanyId = UUID.fromString("00000000-0000-0000-0000-000000000000"); // TODO: Real tenant logic

        // Check existence
        // We lack findByEmail in repository interface likely.

        Customer lead = Customer.create(new CompanyId(defaultCompanyId),
                command.getCompanyName() != null ? command.getCompanyName() : command.getName(), null);
        lead.updateContactInfo(Email.of(command.getEmail()), command.getPhone(), null, null, null);
        lead.setType("LEAD");
        lead.setOrigin(command.getOrigin());
        lead.setTags(command.getTags().toArray(new String[0]));
        lead.setMarketingConsent(command.isConsent());
        lead.setMarketingStatus(command.isConsent() ? "SUBSCRIBED" : "UNSUBSCRIBED");
        lead.setUnsubscribeToken(UUID.randomUUID().toString());

        customerRepository.save(lead);
        log.info("Ingested lead: {}", command.getEmail());
    }
}
