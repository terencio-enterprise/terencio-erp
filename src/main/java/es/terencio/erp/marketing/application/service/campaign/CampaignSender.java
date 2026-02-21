package es.terencio.erp.marketing.application.service.campaign;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingStatus;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;
import es.terencio.erp.shared.domain.query.PageResult;

public class CampaignSender {
    private static final Logger log = LoggerFactory.getLogger(CampaignSender.class);

    private final CampaignRepositoryPort campaignRepository;
    private final MailingSystemPort mailingSystem;
    private final EmailContentBuilder contentBuilder;
    private final MarketingProperties properties;

    public CampaignSender(CampaignRepositoryPort campaignRepository, MailingSystemPort mailingSystem,
            EmailContentBuilder contentBuilder, MarketingProperties properties) {
        this.campaignRepository = campaignRepository;
        this.mailingSystem = mailingSystem;
        this.contentBuilder = contentBuilder;
        this.properties = properties;
    }

    @Async
    public void executeCampaign(UUID companyId, Long campaignId, boolean isRelaunch) {
        boolean acquired = campaignRepository.tryStartCampaign(campaignId, isRelaunch);
        if (!acquired) {
            log.warn(
                    "Execution Aborted: Campaign {} could not be locked. It is already running or in an invalid state.",
                    campaignId);
            return;
        }

        MarketingCampaign campaign;
        MarketingTemplate tpl;
        try {
            campaign = campaignRepository.findCampaignById(campaignId).orElseThrow();
            tpl = campaignRepository.findTemplateById(campaign.getTemplateId()).orElseThrow();
        } catch (Exception e) {
            log.error("Execution failed: Entities not found for campaign {}", campaignId, e);
            return;
        }

        PageResult<CampaignAudienceMember> firstPage = campaignRepository.findCampaignAudience(companyId, campaignId, 0,
                1);
        int totalRecipients = (int) firstPage.totalElements();
        campaignRepository.updateCampaignTotalRecipients(campaignId, totalRecipients);

        CampaignRateLimiter rateLimiter = new CampaignRateLimiter(properties.getRateLimitPerSecond());
        EmailRetryPolicy retryPolicy = new EmailRetryPolicy(properties.getMaxRetries());

        long lastSeenCustomerId = 0L;
        int sentInThisSession = 0;

        while (true) {
            var batch = campaignRepository.findCampaignAudienceBatch(
                    campaign.getCompanyId(), campaign.getId(), lastSeenCustomerId, properties.getBatchSize());

            if (batch == null || batch.isEmpty())
                break;

            for (CampaignAudienceMember member : batch) {
                lastSeenCustomerId = member.customerId();
                boolean isSubscribed = member.marketingStatus() == MarketingStatus.SUBSCRIBED;
                boolean shouldSend;

                if (isRelaunch) {
                    shouldSend = member.sendStatus() == DeliveryStatus.NOT_SENT
                            || member.sendStatus() == DeliveryStatus.FAILED;
                } else {
                    shouldSend = member.sendStatus() == null
                            || member.sendStatus() == DeliveryStatus.NOT_SENT;
                }

                if (!isSubscribed || !shouldSend)
                    continue;

                rateLimiter.acquire();

                try {
                    boolean success = processSingleCustomer(campaign, tpl, member, retryPolicy);
                    if (success) {
                        sentInThisSession++;
                    }
                } catch (DataIntegrityViolationException e) {
                    log.warn("DB Idempotency: Duplicate log prevented for campaign {} and customer {}",
                            campaign.getId(), member.customerId());
                } catch (Exception e) {
                    log.error("Unexpected error processing customer {}", member.customerId(), e);
                }
            }
        }

        campaignRepository.completeCampaign(campaignId, sentInThisSession);
        log.info("Campaign {} execution finished. Emails sent this session: {}", campaignId, sentInThisSession);
    }

    private boolean processSingleCustomer(MarketingCampaign campaign, MarketingTemplate tpl,
            CampaignAudienceMember member, EmailRetryPolicy retryPolicy) {
        CampaignLog logEntry = CampaignLog.createPending(campaign.getId(), campaign.getCompanyId(), member.customerId(),
                tpl.getId());
        campaignRepository.saveLog(logEntry);

        return retryPolicy.execute(() -> {
            try {
                EmailMessage msg = contentBuilder.buildMessage(tpl, member, logEntry.getId());
                String messageId = mailingSystem.send(msg);

                logEntry.markSent(messageId);
                campaignRepository.saveLog(logEntry);
                return true;
            } catch (Exception e) {
                logEntry.markFailed(e.getMessage());
                campaignRepository.saveLog(logEntry);
                throw e;
            }
        }, member.email());
    }
}