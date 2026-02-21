package es.terencio.erp.crm.application.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import es.terencio.erp.crm.application.port.in.UpdateSpecialPricesUseCase;
import es.terencio.erp.crm.application.port.out.CustomerRepository;
import es.terencio.erp.crm.application.port.out.SpecialPriceRepository;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.crm.domain.model.CustomerProductPrice;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.ProductId;
import es.terencio.erp.shared.domain.valueobject.Email;
import es.terencio.erp.shared.domain.valueobject.Money;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerApplicationService implements IngestLeadUseCase {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void ingest(UUID companyId, LeadCommand command) {
        CompanyId cid = new CompanyId(companyId);
        Email email = Email.of(command.email());

        if (customerRepository.existsByEmail(cid, email)) {
            log.info("Lead {} already exists for company {}", email, companyId);
            return;
        }

        String name = (command.companyName() != null && !command.companyName().isBlank())
                ? command.companyName()
                : command.name();

        Customer lead = Customer.createLead(cid, name, email, command.origin(), command.tags());

        // Enrich with phone if provided
        if (command.phone() != null) {
            lead.getContactInfo().setPhone(command.phone());
        }

        customerRepository.save(lead);
        log.info("Lead ingested: {} (UUID: {})", email, lead.getUuid());
    }

    @Override
    @Transactional
    public void updatePrices(UUID customerUuid, SpecialPricesCommand command) {
        Customer customer = customerRepository.findByUuid(customerUuid)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        for (var entry : command.prices()) {
            CustomerProductPrice price = CustomerProductPrice.create(
                    customer.getId(),
                    new ProductId(entry.productId()),
                    Money.ofEurosCents(entry.priceCents()));
            priceRepository.save(price);
        }
    }
}