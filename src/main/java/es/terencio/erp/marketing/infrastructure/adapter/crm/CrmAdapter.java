package es.terencio.erp.marketing.infrastructure.adapter.crm;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import es.terencio.erp.crm.application.port.out.CustomerRepository;
import es.terencio.erp.crm.domain.model.Customer;
import es.terencio.erp.marketing.application.dto.CampaignRequest;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CrmAdapter implements CustomerIntegrationPort {

    private final CustomerRepository customerRepository;

    @Override
    public List<MarketingCustomer> findAudience(CampaignRequest.AudienceFilter filter) {
        // We use the new repository method
        List<Customer> customers = customerRepository.findByMarketingCriteria(
                filter.getTags(),
                filter.getCustomerType(),
                filter.getMinSpent());

        return customers.stream()
                .map(this::mapToMarketingCustomer)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<MarketingCustomer> findByToken(String token) {
        return customerRepository.findByUnsubscribeToken(token)
                .map(this::mapToMarketingCustomer);
    }

    @Override
    public void updateMarketingStatus(String token, String status, Instant snoozedUntil) {
        customerRepository.findByUnsubscribeToken(token).ifPresent(customer -> {
            customer.setMarketingStatus(status);
            customer.setSnoozedUntil(snoozedUntil);
            customerRepository.save(customer);
        });
    }

    private MarketingCustomer mapToMarketingCustomer(Customer c) {
        return MarketingCustomer.builder()
                .id(c.id().value())
                .companyId(c.companyId().value())
                .email(c.email() != null ? c.email().value() : null)
                .name(c.legalName()) // or commercial name
                .token(c.getUnsubscribeToken())
                .unsubscribeToken(c.getUnsubscribeToken())
                .canReceiveMarketing(c.canReceiveMarketing())
                .build();
    }
}
