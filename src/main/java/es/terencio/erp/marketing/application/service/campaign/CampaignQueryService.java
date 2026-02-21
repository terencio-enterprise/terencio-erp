package es.terencio.erp.marketing.application.service.campaign;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.campaign.CampaignResponse;
import es.terencio.erp.marketing.application.port.in.CampaignQueryUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class CampaignQueryService implements CampaignQueryUseCase {

    private final CampaignRepositoryPort campaignRepository;

    public CampaignQueryService(CampaignRepositoryPort campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getCampaign(UUID companyId, Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }
        return new CampaignResponse(campaign.getId(), campaign.getName(), campaign.getStatus(), campaign.getScheduledAt(),
                campaign.getTotalRecipients(), campaign.getSent(), campaign.getDelivered(), campaign.getOpened(),
                campaign.getClicked(), campaign.getBounced(), campaign.getUnsubscribed());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CampaignAudienceMember> getCampaignAudience(UUID companyId, Long campaignId, int page, int size) {
        return campaignRepository.findCampaignAudience(companyId, campaignId, page, size);
    }
}