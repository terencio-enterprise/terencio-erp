package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import es.terencio.erp.marketing.application.dto.MarketingDtos.AudienceFilter;

public interface CustomerIntegrationPort {
    List<MarketingCustomer> findAudience(AudienceFilter filter);
    Optional<MarketingCustomer> findByToken(String token);
    void updateMarketingStatus(String token, String status, Instant snoozedUntil);

    record MarketingCustomer(Long id, UUID companyId, String name, String email, String unsubscribeToken, boolean canReceiveMarketing) {}
}