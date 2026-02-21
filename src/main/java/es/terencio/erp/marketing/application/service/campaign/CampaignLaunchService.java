package es.terencio.erp.marketing.application.service.campaign;

import java.util.Map;
import java.util.UUID;

import es.terencio.erp.marketing.application.port.in.CampaignLaunchUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.application.port.out.TemplateEnginePort;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class CampaignLaunchService implements CampaignLaunchUseCase {

    private final CampaignSender campaignSender;
    private final CampaignRepositoryPort campaignRepository;
    private final MailingSystemPort mailingSystem;
    private final TemplateEnginePort templateEngine;
    private final MarketingProperties properties;

    public CampaignLaunchService(CampaignSender campaignSender, CampaignRepositoryPort campaignRepository,
            MailingSystemPort mailingSystem, TemplateEnginePort templateEngine, MarketingProperties properties) {
        this.campaignSender = campaignSender;
        this.campaignRepository = campaignRepository;
        this.mailingSystem = mailingSystem;
        this.templateEngine = templateEngine;
        this.properties = properties;
    }

    @Override
    public void launchCampaign(UUID companyId, Long campaignId) {
        campaignSender.executeCampaign(companyId, campaignId, false);
    }

    @Override
    public void relaunchCampaign(UUID companyId, Long campaignId) {
        campaignSender.executeCampaign(companyId, campaignId, true);
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
        
        String body = tpl.compile(vars, templateEngine);
        String subject = tpl.compileSubject(vars, templateEngine);
        
        EmailMessage msg = EmailMessage.of(testEmail, subject, body, "test-token");
        mailingSystem.send(msg);
    }
}