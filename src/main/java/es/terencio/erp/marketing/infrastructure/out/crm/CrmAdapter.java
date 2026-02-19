package es.terencio.erp.marketing.infrastructure.out.crm;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import es.terencio.erp.crm.application.port.out.CustomerRepository;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.marketing.application.dto.MarketingDtos.AudienceFilter;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;

@Component
public class CrmAdapter implements CustomerIntegrationPort {
    private final CustomerRepository customerRepository;

    public CrmAdapter(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public List<MarketingCustomer> findAudience(AudienceFilter filter) {
        List<Customer> customers = customerRepository.findByMarketingCriteria(filter.tags(), filter.customerType(), filter.minSpent());
        return customers.stream().map(this::mapToMarketingCustomer).collect(Collectors.toList());
    }

    @Override
    public Optional<MarketingCustomer> findByToken(String token) {
        return customerRepository.findByUnsubscribeToken(token).map(this::mapToMarketingCustomer);
    }

    @Override
    public void updateMarketingStatus(String token, String status, Instant snoozedUntil) {
        customerRepository.findByUnsubscribeToken(token).ifPresent(customer -> {
            customer.setSnoozedUntil(snoozedUntil);
            customerRepository.save(customer);
        });
    }

    private MarketingCustomer mapToMarketingCustomer(Customer c) {
        return new MarketingCustomer(c.id().value(), c.companyId().value(), c.email() != null ? c.email().value() : null, c.legalName(), c.getUnsubscribeToken(), c.getUnsubscribeToken(), c.canReceiveMarketing());
    }
}
