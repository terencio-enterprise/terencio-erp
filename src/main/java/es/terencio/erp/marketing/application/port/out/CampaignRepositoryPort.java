package es.terencio.erp.marketing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

public interface CampaignRepositoryPort {
    // Templates
    Optional<MarketingTemplate> findTemplateById(Long id);
    List<MarketingTemplate> findAllTemplates(UUID companyId, String search);
    MarketingTemplate saveTemplate(MarketingTemplate template);
    void deleteTemplate(Long id);

    // Campaigns
    Optional<MarketingCampaign> findCampaignById(Long id);
    MarketingCampaign saveCampaign(MarketingCampaign campaign);
    void incrementCampaignMetric(Long campaignId, String metricType);

    // Logs
    CampaignLog saveLog(CampaignLog log);
    Optional<CampaignLog> findLogById(Long id);
    List<CampaignLog> findLogsByStatus(String status);
    boolean hasLog(Long campaignId, Long customerId);
}
