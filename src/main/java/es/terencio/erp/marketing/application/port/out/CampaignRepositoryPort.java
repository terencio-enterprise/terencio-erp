package es.terencio.erp.marketing.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;

public interface CampaignRepositoryPort {
    Optional<MarketingTemplate> findTemplateById(Long id);
    List<MarketingTemplate> findAllTemplates(UUID companyId, String search);
    MarketingTemplate saveTemplate(MarketingTemplate template);
    void deleteTemplate(Long id);
    void saveLog(CampaignLog log);
    List<CampaignLog> findLogsByStatus(String status);
}