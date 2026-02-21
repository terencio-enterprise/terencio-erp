package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.Optional;

import es.terencio.erp.marketing.domain.model.MarketingStatus;

public interface CustomerIntegrationPort {
    
    public record MarketingCustomer(
        Long id,
        String email,
        String name,
        boolean canReceiveMarketing,
        MarketingStatus marketingStatus,
        String unsubscribeToken
    ) {}

    Optional<MarketingCustomer> findByToken(String token);

    void updateMarketingStatus(String token, MarketingStatus status, Instant snoozeUntil);
}