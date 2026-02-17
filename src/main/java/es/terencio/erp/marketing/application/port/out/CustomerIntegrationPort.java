package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.CampaignRequest;
import lombok.Builder;
import lombok.Data;

public interface CustomerIntegrationPort {
    List<MarketingCustomer> findAudience(CampaignRequest.AudienceFilter filter);

    Optional<MarketingCustomer> findByToken(String token);

    void updateMarketingStatus(String token, String status, Instant snoozedUntil);

    @Data
    @Builder
    class MarketingCustomer {
        private Long id;
        private UUID companyId;
        private String email;
        private String name;
        private String token;
        private String unsubscribeToken;
        private boolean canReceiveMarketing;
    }
}
