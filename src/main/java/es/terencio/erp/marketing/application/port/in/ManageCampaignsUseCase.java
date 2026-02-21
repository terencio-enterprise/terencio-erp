package es.terencio.erp.marketing.application.port.in;

import java.util.UUID;

import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignResponse;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CreateCampaignRequest;
import es.terencio.erp.shared.domain.query.PageResult;

public interface ManageCampaignsUseCase {
    CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request);
    CampaignResponse updateDraft(UUID companyId, Long campaignId, CreateCampaignRequest request);
    CampaignResponse getCampaign(UUID companyId, Long campaignId);
    PageResult<CampaignAudienceMember> getCampaignAudience(UUID companyId, Long campaignId, int page, int size);
}