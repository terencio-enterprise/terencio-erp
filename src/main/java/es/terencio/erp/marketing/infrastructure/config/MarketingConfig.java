package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import es.terencio.erp.marketing.application.port.in.*;
import es.terencio.erp.marketing.application.port.out.*;
import es.terencio.erp.marketing.application.service.*;

@Configuration
public class MarketingConfig {

    @Bean
    public CampaignService campaignService(CampaignRepositoryPort campaignRepository, 
                                           CustomerIntegrationPort customerPort, 
                                           MailingSystemPort mailingSystem,
                                           @Value("${app.public-url:https://api.terencio.es}") String publicBaseUrl,
                                           @Value("${app.marketing.hmac-secret:ChangeMeForProduction!123}") String hmacSecret) {
        return new CampaignService(campaignRepository, customerPort, mailingSystem, publicBaseUrl, hmacSecret);
    }

    @Bean
    public ManageCampaignsUseCase manageCampaignsUseCase(CampaignService campaignService) { return campaignService; }

    @Bean
    public CampaignTrackingUseCase campaignTrackingUseCase(CampaignService campaignService) { return campaignService; }

    @Bean
    public ManagePreferencesUseCase managePreferencesUseCase(CustomerIntegrationPort customerPort) { return new PreferenceService(customerPort); }

    @Bean
    public ManageTemplatesUseCase manageTemplatesUseCase(CampaignRepositoryPort repository) { return new TemplateService(repository); }
    
    @Bean
    public ProcessWebhookUseCase processWebhookUseCase(CampaignRepositoryPort repository) { return new WebhookService(repository); }
}
