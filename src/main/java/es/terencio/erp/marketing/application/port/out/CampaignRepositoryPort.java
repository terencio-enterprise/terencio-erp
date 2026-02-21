package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignAudienceMember;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.domain.query.PageResult;

public interface CampaignRepositoryPort {
    // Lock
    boolean acquireSchedulerLock(String lockName);
    void releaseSchedulerLock(String lockName);

    // Templates
    Optional<MarketingTemplate> findTemplateById(Long id);
    List<MarketingTemplate> findAllTemplates(UUID companyId, String search);
    MarketingTemplate saveTemplate(MarketingTemplate template);
    void deleteTemplate(Long id);

    // Campaigns
    Optional<MarketingCampaign> findCampaignById(Long id);
    List<MarketingCampaign> findScheduledCampaignsToLaunch(Instant now);
    MarketingCampaign saveCampaign(MarketingCampaign campaign);
    void incrementCampaignMetric(Long campaignId, String metricType);
    int countDailySendsForCompany(UUID companyId, Instant startOfDay);
    PageResult<CampaignAudienceMember> findCampaignAudience(UUID companyId, Long campaignId, int page, int size);

    // Logs & Events
    CampaignLog saveLog(CampaignLog log);
    Optional<CampaignLog> findLogById(Long id);
    Optional<CampaignLog> findLogByMessageId(String messageId);
    boolean hasLog(Long campaignId, Long customerId);
    
    // Webhook Support
    void saveDeliveryEvent(EmailDeliveryEvent event);
    void markCustomerAsBouncedOrComplained(Long customerId, String status);
}
