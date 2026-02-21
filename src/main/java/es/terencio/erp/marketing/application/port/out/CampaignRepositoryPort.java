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

    // Campaigns
    Optional<MarketingCampaign> findCampaignById(Long campaignId);
    MarketingCampaign saveCampaign(MarketingCampaign campaign);
    PageResult<MarketingCampaign> findCampaigns(UUID companyId, String search, String status, int page, int size);
    List<MarketingCampaign> findScheduledCampaignsToLaunch(Instant now);
    
    // Audience & Execution
    PageResult<CampaignAudienceMember> findCampaignAudience(UUID companyId, Long campaignId, int page, int size);
    boolean tryStartCampaign(Long campaignId, boolean isRelaunch);
    void updateCampaignTotalRecipients(Long campaignId, int totalRecipients);
    void completeCampaign(Long campaignId, int sentInThisSession);
    void incrementCampaignMetric(Long campaignId, String metricName);
    
    // Logs & Metrics
    PageResult<CampaignLogResponse> findCampaignLogs(UUID companyId, Long campaignId, String status, int page, int size);
    void saveLog(CampaignLog logEntry);
    Optional<CampaignLog> findLogById(Long logId);
    Optional<CampaignLog> findLogByMessageId(String messageId);
    
    // Templates
    Optional<MarketingTemplate> findTemplateById(Long templateId);
    List<MarketingTemplate> findAllTemplates(UUID companyId, String search);
    MarketingTemplate saveTemplate(MarketingTemplate template);
    void deleteTemplate(Long id);

    // Concurrency
    boolean acquireSchedulerLock(String lockName);
    void releaseSchedulerLock(String lockName);
    
    // Webhooks & Events
    void saveDeliveryEvent(EmailDeliveryEvent event);
    void markCustomerAsBouncedOrComplained(Long customerId, String type);
}