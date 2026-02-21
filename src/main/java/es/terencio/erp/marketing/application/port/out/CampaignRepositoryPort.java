package es.terencio.erp.marketing.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.domain.query.PageResult;

public interface CampaignRepositoryPort {
    
    // --- ATOMIC OPERATIONS FOR SAFE EXECUTION ---

    boolean tryStartCampaign(Long campaignId, boolean isRelaunch);
    
    void updateCampaignTotalRecipients(Long campaignId, int totalRecipients);

    void completeCampaign(Long campaignId, int sentInThisSession);

    // --- STANDARD CRUD & QUERIES ---

    Optional<MarketingCampaign> findCampaignById(Long id);
    MarketingCampaign saveCampaign(MarketingCampaign campaign);
    
    PageResult<CampaignAudienceMember> findCampaignAudience(UUID companyId, Long campaignId, int page, int size);
    List<MarketingCampaign> findScheduledCampaignsToLaunch(Instant now);
    
    Optional<MarketingTemplate> findTemplateById(Long id);
    List<MarketingTemplate> findAllTemplates(UUID companyId, String search);
    MarketingTemplate saveTemplate(MarketingTemplate template);
    void deleteTemplate(Long id);
    
    void saveLog(CampaignLog logEntry);
    Optional<CampaignLog> findLogById(Long logId);
    Optional<CampaignLog> findLogByMessageId(String messageId);
    void incrementCampaignMetric(Long campaignId, String metricName);
    
    void saveDeliveryEvent(EmailDeliveryEvent event);
    void markCustomerAsBouncedOrComplained(Long customerId, String status);
    
    boolean acquireSchedulerLock(String lockName);
    void releaseSchedulerLock(String lockName);
}