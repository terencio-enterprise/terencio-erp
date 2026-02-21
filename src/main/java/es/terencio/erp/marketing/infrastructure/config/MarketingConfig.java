package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.marketing.application.port.in.CampaignTrackingUseCase;
import es.terencio.erp.marketing.application.port.in.ManageCampaignsUseCase;
import es.terencio.erp.marketing.application.port.in.ManagePreferencesUseCase;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.marketing.application.port.in.ProcessWebhookUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.application.service.CampaignExecutionWorker;
import es.terencio.erp.marketing.application.service.CampaignService;
import es.terencio.erp.marketing.application.service.CampaignTrackingService;
import es.terencio.erp.marketing.application.service.PreferenceService;
import es.terencio.erp.marketing.application.service.TemplateService;
import es.terencio.erp.marketing.application.service.WebhookService;

@Configuration
public class MarketingConfig {

    @Bean
    public CampaignExecutionWorker campaignExecutionWorker(
            CampaignRepositoryPort campaignRepository,
            MailingSystemPort mailingSystem,
            MarketingProperties properties
    ) {
        return new CampaignExecutionWorker(campaignRepository, mailingSystem, properties);
    }

    @Bean
    public CampaignTrackingService campaignTrackingService(
            CampaignRepositoryPort campaignRepository,
            MarketingProperties properties
    ) {
        return new CampaignTrackingService(campaignRepository, properties);
    }

    @Bean
    public CampaignService campaignService(
            CampaignRepositoryPort campaignRepository,
            CampaignExecutionWorker executionWorker,
            MailingSystemPort mailingSystem,
            MarketingProperties properties,
            ObjectMapper objectMapper
    ) {
        return new CampaignService(campaignRepository, executionWorker, mailingSystem, properties, objectMapper);
    }

    @Bean
    public ManageCampaignsUseCase manageCampaignsUseCase(CampaignService service) {
        return service;
    }

    @Bean
    public CampaignTrackingUseCase campaignTrackingUseCase(CampaignTrackingService service) {
        return service;
    }

    @Bean
    public ManagePreferencesUseCase managePreferencesUseCase(CustomerIntegrationPort customerPort) {
        return new PreferenceService(customerPort);
    }

    @Bean
    public ManageTemplatesUseCase manageTemplatesUseCase(CampaignRepositoryPort repository) {
        return new TemplateService(repository);
    }

    @Bean
    public ProcessWebhookUseCase processWebhookUseCase(CampaignRepositoryPort repository, ObjectMapper objectMapper) {
        return new WebhookService(repository, objectMapper);
    }
}