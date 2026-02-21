package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.Optional;

import es.terencio.erp.marketing.application.dto.customer.MarketingCustomer;
import es.terencio.erp.marketing.domain.model.MarketingStatus;

public interface CustomerIntegrationPort {
    Optional<MarketingCustomer> findByToken(String token);
    void updateMarketingStatus(String token, MarketingStatus status, Instant snoozeUntil);
}