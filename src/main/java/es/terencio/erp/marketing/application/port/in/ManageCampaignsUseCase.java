package es.terencio.erp.marketing.application.port.in;

import java.util.List;
import java.util.UUID;
import es.terencio.erp.marketing.application.dto.MarketingDtos.*;

public interface ManageCampaignsUseCase {
    CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request);
    CampaignResponse getCampaign(Long campaignId);
    List<CampaignAudienceMember> getCampaignAudience(Long campaignId);
    
    void launchCampaign(Long campaignId);
    void relaunchCampaign(Long campaignId); // Idempotent
    void scheduleCampaign(Long campaignId, java.time.Instant scheduledAt);
    void dryRun(Long templateId, String testEmail);
}
