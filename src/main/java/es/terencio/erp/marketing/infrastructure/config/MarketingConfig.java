package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.application.port.out.MarketingSettingsRepositoryPort;
import es.terencio.erp.marketing.application.port.out.TemplateEnginePort;
import es.terencio.erp.marketing.application.service.campaign.CampaignCommandService;
import es.terencio.erp.marketing.application.service.campaign.CampaignLaunchService;
import es.terencio.erp.marketing.application.service.campaign.CampaignQueryService;
import es.terencio.erp.marketing.application.service.campaign.CampaignSender;
import es.terencio.erp.marketing.application.service.campaign.EmailContentBuilder;
import es.terencio.erp.marketing.application.service.campaign.TrackingLinkService;
import es.terencio.erp.marketing.application.service.preference.CustomerPreferenceService;
import es.terencio.erp.marketing.application.service.settings.MarketingSettingsService;
import es.terencio.erp.marketing.application.service.template.TemplateService;
import es.terencio.erp.marketing.application.service.tracking.CampaignTrackingService;
import es.terencio.erp.marketing.application.service.webhook.SesWebhookService;
import es.terencio.erp.marketing.infrastructure.out.template.SimpleTemplateEngineAdapter;

@Configuration
public class MarketingConfig {

    @Bean
    public TemplateEnginePort templateEnginePort() {
        return new SimpleTemplateEngineAdapter();
    }

    @Bean
    public TrackingLinkService trackingLinkService(MarketingProperties properties) {
        return new TrackingLinkService(properties);
    }

    @Bean
    public EmailContentBuilder emailContentBuilder(TemplateEnginePort templateEngine, TrackingLinkService trackingLinkService, MarketingProperties properties) {
        return new EmailContentBuilder(templateEngine, trackingLinkService, properties);
    }

    @Bean
    public CampaignSender campaignSender(
            CampaignRepositoryPort campaignRepository,
            MailingSystemPort mailingSystem,
            EmailContentBuilder contentBuilder,
            MarketingProperties properties
    ) {
        return new CampaignSender(campaignRepository, mailingSystem, contentBuilder, properties);
    }

    @Bean
    public CampaignTrackingService campaignTrackingService(
            CampaignRepositoryPort campaignRepository,
            TrackingLinkService trackingLinkService,
            MarketingProperties properties
    ) {
        return new CampaignTrackingService(campaignRepository, trackingLinkService, properties);
    }

    @Bean
    public CampaignCommandService campaignCommandService(CampaignRepositoryPort campaignRepository) {
        return new CampaignCommandService(campaignRepository);
    }

    @Bean
    public CampaignQueryService campaignQueryService(CampaignRepositoryPort campaignRepository) {
        return new CampaignQueryService(campaignRepository);
    }

    @Bean
    public CampaignLaunchService campaignLaunchService(
            CampaignSender campaignSender,
            CampaignRepositoryPort campaignRepository,
            MailingSystemPort mailingSystem,
            TemplateEnginePort templateEngine,
            MarketingProperties properties
    ) {
        return new CampaignLaunchService(campaignSender, campaignRepository, mailingSystem, templateEngine, properties);
    }

    @Bean
    public CustomerPreferenceService customerPreferenceService(CustomerIntegrationPort customerPort) {
        return new CustomerPreferenceService(customerPort);
    }

    @Bean
    public TemplateService templateService(CampaignRepositoryPort repository, TemplateEnginePort templateEngine) {
        return new TemplateService(repository, templateEngine);
    }

    @Bean
    public MarketingSettingsService marketingSettingsService(MarketingSettingsRepositoryPort repository) {
        return new MarketingSettingsService(repository);
    }

    @Bean
    public SesWebhookService sesWebhookService(CampaignRepositoryPort repository, ObjectMapper objectMapper, RestTemplate restTemplate) {
        return new SesWebhookService(repository, objectMapper, restTemplate);
    }
}