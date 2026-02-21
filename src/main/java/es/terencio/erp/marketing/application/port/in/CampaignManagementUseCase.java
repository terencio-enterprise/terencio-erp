package es.terencio.erp.marketing.application.port.in;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.campaign.CampaignResponse;
import es.terencio.erp.marketing.application.dto.campaign.CreateCampaignRequest;

public interface CampaignManagementUseCase {
    CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request);
    CampaignResponse updateDraft(UUID companyId, Long campaignId, CreateCampaignRequest request);
    void scheduleCampaign(UUID companyId, Long campaignId, Instant scheduledAt);
    void cancelCampaign(UUID companyId, Long campaignId);
}