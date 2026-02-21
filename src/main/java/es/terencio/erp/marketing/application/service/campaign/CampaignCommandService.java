package es.terencio.erp.marketing.application.service.campaign;

import java.time.Instant;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.campaign.AudienceFilterDto;
import es.terencio.erp.marketing.application.dto.campaign.CampaignResponse;
import es.terencio.erp.marketing.application.dto.campaign.CreateCampaignRequest;
import es.terencio.erp.marketing.application.port.in.CampaignManagementUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.AudienceFilter;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class CampaignCommandService implements CampaignManagementUseCase {

    private final CampaignRepositoryPort campaignRepository;

    public CampaignCommandService(CampaignRepositoryPort campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    private MarketingCampaign getCampaignEntity(UUID companyId, Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }
        return campaign;
    }

    private AudienceFilter mapFilter(AudienceFilterDto dto) {
        if (dto == null) return null;
        return new AudienceFilter(dto.tags(), dto.minSpent(), dto.customerType());
    }

    @Override
    @Transactional
    public CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request) {
        AudienceFilter filter = mapFilter(request.audienceFilter());
        MarketingCampaign campaign = MarketingCampaign.createDraft(companyId, request.name(), request.templateId(), filter);
        MarketingCampaign saved = campaignRepository.saveCampaign(campaign);
        return toCampaignDto(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateDraft(UUID companyId, Long campaignId, CreateCampaignRequest request) {
        MarketingCampaign campaign = getCampaignEntity(companyId, campaignId);
        AudienceFilter filter = mapFilter(request.audienceFilter());
        
        campaign.updateDraft(request.name(), request.templateId(), filter);
        return toCampaignDto(campaignRepository.saveCampaign(campaign));
    }

    @Override
    @Transactional
    public void scheduleCampaign(UUID companyId, Long campaignId, Instant s) {
        MarketingCampaign campaign = getCampaignEntity(companyId, campaignId);
        campaign.schedule(s);
        campaignRepository.saveCampaign(campaign);
    }
    
    @Override
    @Transactional
    public void cancelCampaign(UUID companyId, Long campaignId) {
        MarketingCampaign campaign = getCampaignEntity(companyId, campaignId);
        campaign.cancel();
        campaignRepository.saveCampaign(campaign);
    }

    private CampaignResponse toCampaignDto(MarketingCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getStatus(), c.getScheduledAt(),
                c.getTotalRecipients(), c.getSent(), c.getDelivered(), c.getOpened(),
                c.getClicked(), c.getBounced(), c.getUnsubscribed());
    }
}