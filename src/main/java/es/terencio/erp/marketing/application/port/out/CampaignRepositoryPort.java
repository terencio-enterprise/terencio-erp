package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.campaign.CampaignLogResponse;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.domain.query.PageResult;

public interface CampaignRepositoryPort {
    // Campaign Management
    Optional<MarketingCampaign> findCampaignById(Long id);
    MarketingCampaign saveCampaign(MarketingCampaign campaign);
    PageResult<MarketingCampaign> findCampaigns(UUID companyId, String search, String status, int page, int size);
    
    // Templates
    Optional<MarketingTemplate> findTemplateById(Long id);
    MarketingTemplate saveTemplate(MarketingTemplate template);
    List<MarketingTemplate> findAllTemplates(UUID companyId, String search);
    void deleteTemplate(Long id);
    
    // Execution & Sending
    boolean tryStartCampaign(Long campaignId, boolean isRelaunch);
    void updateCampaignTotalRecipients(Long campaignId, int total);
    PageResult<CampaignAudienceMember> findCampaignAudience(UUID companyId, Long campaignId, int page, int size);
    void completeCampaign(Long campaignId, int sentCount);
    
    // Scheduler
    boolean acquireSchedulerLock(String lockName);
    void releaseSchedulerLock(String lockName);
    List<MarketingCampaign> findScheduledCampaignsToLaunch(Instant now);
    
    // Logs & Metrics
    void saveLog(CampaignLog log);
    Optional<CampaignLog> findLogById(Long id);
    Optional<CampaignLog> findLogByMessageId(String messageId);
    PageResult<CampaignLogResponse> findCampaignLogs(UUID companyId, Long campaignId, String status, int page, int size);
    
    void incrementCampaignMetric(Long campaignId, String metric);
    void markCustomerAsBouncedOrComplained(Long customerId, String type);
    void saveDeliveryEvent(EmailDeliveryEvent event);
}