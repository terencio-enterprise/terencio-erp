package es.terencio.erp.marketing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.domain.model.Campaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

public interface CampaignRepositoryPort {
    // Template Management
    List<MarketingTemplate> findAllTemplates(UUID companyId, String search);

    Optional<MarketingTemplate> findTemplateById(Long id);

    MarketingTemplate saveTemplate(MarketingTemplate template);

    void deleteTemplate(Long id);

    // Campaign Logs
    Campaign saveLog(Campaign log);

    List<Campaign> findLogsByStatus(String status);
}
