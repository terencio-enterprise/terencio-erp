package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignResponse;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CreateCampaignRequest;
import es.terencio.erp.marketing.application.port.in.ManageCampaignsUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class CampaignService implements ManageCampaignsUseCase {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepositoryPort campaignRepository;
    private final CampaignExecutionWorker executionWorker;
    private final MailingSystemPort mailingSystem;
    private final MarketingProperties properties;
    private final ObjectMapper objectMapper;

    public CampaignService(CampaignRepositoryPort campaignRepository, 
                           CampaignExecutionWorker executionWorker,
                           MailingSystemPort mailingSystem,
                           MarketingProperties properties, 
                           ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.executionWorker = executionWorker;
        this.mailingSystem = mailingSystem;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private MarketingCampaign getCampaignEntity(UUID companyId, Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }
        return campaign;
    }

    @Override
    @Transactional
    public CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request) {
        String filterJson = null;
        try {
            if (request.audienceFilter() != null) {
                filterJson = objectMapper.writeValueAsString(request.audienceFilter());
            }
        } catch (Exception e) { log.warn("Failed to serialize audience filter", e); }

        MarketingCampaign campaign = MarketingCampaign.createDraft(companyId, request.name(), request.templateId(), filterJson);
        MarketingCampaign saved = campaignRepository.saveCampaign(campaign);
        return toCampaignDto(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateDraft(UUID companyId, Long campaignId, CreateCampaignRequest request) {
        MarketingCampaign campaign = getCampaignEntity(companyId, campaignId);
        String filterJson = null;
        try {
            if (request.audienceFilter() != null) {
                filterJson = objectMapper.writeValueAsString(request.audienceFilter());
            }
        } catch (Exception e) { log.warn("Failed to serialize audience filter", e); }
        
        campaign.updateDraft(request.name(), request.templateId(), filterJson);
        return toCampaignDto(campaignRepository.saveCampaign(campaign));
    }

    @Override
    public CampaignResponse getCampaign(UUID companyId, Long campaignId) {
        return toCampaignDto(getCampaignEntity(companyId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CampaignAudienceMember> getCampaignAudience(UUID companyId, Long campaignId, int page, int size) {
        return campaignRepository.findCampaignAudience(companyId, campaignId, page, size);
    }

    @Override
    public void launchCampaign(UUID companyId, Long campaignId) {
        executionWorker.executeCampaign(companyId, campaignId, false);
    }

    @Override
    public void relaunchCampaign(UUID companyId, Long campaignId) {
        executionWorker.executeCampaign(companyId, campaignId, true);
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

    @Override
    public void dryRun(UUID companyId, Long templateId, String testEmail) {
        MarketingTemplate tpl = campaignRepository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
                
        if (!tpl.getCompanyId().equals(companyId)) {
            throw new InvariantViolationException("Template access denied");
        }
        
        Map<String, String> vars = Map.of(
                "name", "Jane Doe",
                "unsubscribe_link", properties.getPublicBaseUrl() + "/api/v1/public/marketing/preferences?token=test-token"
        );
        String body = tpl.compile(vars);
        EmailMessage msg = EmailMessage.of(testEmail, tpl.compileSubject(vars), body, "test-token");
        mailingSystem.send(msg);
    }

    private CampaignResponse toCampaignDto(MarketingCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getStatus(), c.getScheduledAt(),
                c.getTotalRecipients(), c.getSent(), c.getDelivered(), c.getOpened(),
                c.getClicked(), c.getBounced(), c.getUnsubscribed());
    }
}