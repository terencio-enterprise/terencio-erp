package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import es.terencio.erp.marketing.application.port.in.ManagePreferencesUseCase;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.marketing.application.port.in.ProcessWebhookUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.application.service.CampaignService;
import es.terencio.erp.marketing.application.service.PreferenceService;
import es.terencio.erp.marketing.application.service.TemplateService;
import es.terencio.erp.marketing.application.service.WebhookService;

@Configuration
public class MarketingConfig {

    @Bean
    public CampaignService campaignService(CampaignRepositoryPort campaignRepository,
            CustomerIntegrationPort customerPort,
            MailingSystemPort mailingSystem,
            MarketingProperties properties) {
        return new CampaignService(campaignRepository, customerPort, mailingSystem, properties);
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
    public ProcessWebhookUseCase processWebhookUseCase(CampaignRepositoryPort repository,
            MarketingProperties properties) {
        return new WebhookService(repository, properties);
    }
}
