package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import es.terencio.erp.marketing.application.port.in.LaunchCampaignUseCase;
import es.terencio.erp.marketing.application.port.in.ManagePreferencesUseCase;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.application.service.CampaignService;
import es.terencio.erp.marketing.application.service.PreferenceService;
import es.terencio.erp.marketing.application.service.TemplateService;

@Configuration
public class MarketingConfig {

    @Bean
    public LaunchCampaignUseCase launchCampaignUseCase(CampaignRepositoryPort campaignRepository, CustomerIntegrationPort customerPort, MailingSystemPort mailingSystem) {
        return new CampaignService(campaignRepository, customerPort, mailingSystem);
    }

    @Bean
    public ManagePreferencesUseCase managePreferencesUseCase(CustomerIntegrationPort customerPort) {
        return new PreferenceService(customerPort);
    }

    @Bean
    public ManageTemplatesUseCase manageTemplatesUseCase(CampaignRepositoryPort repository) {
        return new TemplateService(repository);
    }
}
