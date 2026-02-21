package es.terencio.erp.marketing.application.service.campaign;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.campaign.CampaignLogResponse;
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
        MarketingCampaign campaign = getCampaignOrThrow(companyId, campaignId);
        return mapToDto(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CampaignResponse> listCampaigns(UUID companyId, String search, String status, int page, int size) {
        PageResult<MarketingCampaign> rawPage = campaignRepository.findCampaigns(companyId, search, status, page, size);
        
        List<CampaignResponse> mappedContent = rawPage.content().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
                
        return new PageResult<>(mappedContent, rawPage.totalElements(), rawPage.totalPages(), rawPage.pageNumber(), rawPage.pageSize());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CampaignAudienceMember> getCampaignAudience(UUID companyId, Long campaignId, int page, int size) {
        return campaignRepository.findCampaignAudience(companyId, campaignId, page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CampaignLogResponse> getCampaignLogs(UUID companyId, Long campaignId, String status, int page, int size) {
        // Verify campaign exists and belongs to company
        getCampaignOrThrow(companyId, campaignId);
        return campaignRepository.findCampaignLogs(companyId, campaignId, status, page, size);
    }

    private MarketingCampaign getCampaignOrThrow(UUID companyId, Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }
        return campaign;
    }

    private CampaignResponse mapToDto(MarketingCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getStatus(), c.getScheduledAt(),
                c.getTotalRecipients(), c.getSent(), c.getDelivered(), c.getOpened(),
                c.getClicked(), c.getBounced(), c.getUnsubscribed());
    }
}