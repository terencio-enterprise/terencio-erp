package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerIntegrationPort {
    record MarketingCustomer(Long id, UUID companyId, String email, String name, boolean canReceiveMarketing, String unsubscribeToken) {}
    
    List<MarketingCustomer> findAudience(Long campaignId, int limit, int page);
    
    Optional<MarketingCustomer> findByToken(String token);
    void updateMarketingStatus(String token, String status, Instant snoozedUntil);
}
