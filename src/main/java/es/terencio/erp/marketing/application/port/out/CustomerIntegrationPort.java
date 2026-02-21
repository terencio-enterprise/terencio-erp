package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignAudienceMember;
import es.terencio.erp.marketing.domain.model.MarketingStatus;
import es.terencio.erp.shared.domain.query.PageResult;

public interface CustomerIntegrationPort {
    PageResult<CampaignAudienceMember> findAudience(UUID companyId, Long campaignId, int page, int size);
    Optional<CampaignAudienceMember> findByToken(String token);
    void updateMarketingStatus(String token, MarketingStatus status, Instant snoozedUntil);
}