package es.terencio.erp.marketing.application.port.in;

import java.util.UUID;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.campaign.CampaignResponse;
import es.terencio.erp.shared.domain.query.PageResult;

public interface CampaignQueryUseCase {
    CampaignResponse getCampaign(UUID companyId, Long campaignId);
    PageResult<CampaignAudienceMember> getCampaignAudience(UUID companyId, Long campaignId, int page, int size);
}