package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.catalina.util.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Async;

import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignAudienceMember;
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

public class CampaignExecutionWorker {
    private static final Logger log = LoggerFactory.getLogger(CampaignExecutionWorker.class);

    private final CampaignRepositoryPort campaignRepository;
    private final MailingSystemPort mailingSystem;
    private final MarketingProperties properties;

    public CampaignExecutionWorker(CampaignRepositoryPort campaignRepository, MailingSystemPort mailingSystem, MarketingProperties properties) {
        this.campaignRepository = campaignRepository;
        this.mailingSystem = mailingSystem;
        this.properties = properties;
    }

    @Async
    public void executeCampaign(UUID companyId, Long campaignId, boolean isRelaunch) {
        // 1. Atomic DB-Level Lock & Start
        boolean acquired = campaignRepository.tryStartCampaign(campaignId, isRelaunch);
        if (!acquired) {
            log.warn("Execution Aborted: Campaign {} could not be locked. It is already running or in an invalid state.", campaignId);
            return;
        }

        MarketingCampaign campaign;
        MarketingTemplate tpl;
        try {
            campaign = campaignRepository.findCampaignById(campaignId).orElseThrow();
            tpl = campaignRepository.findTemplateById(campaign.getTemplateId()).orElseThrow();
        } catch (Exception e) {
            log.error("Execution failed: Entities not found for campaign {}", campaignId);
            return;
        }

        // 2. Set Total Recipients Before Sending (Accurate Metrics)
        PageResult<CampaignAudienceMember> firstPage = campaignRepository.findCampaignAudience(companyId, campaignId, 0, 1);
        int totalRecipients = (int) firstPage.totalElements();
        campaignRepository.updateCampaignTotalRecipients(campaignId, totalRecipients);

        // 3. Setup Precise Rate Limiting (Guava)
        RateLimiter rateLimiter = RateLimiter.create(properties.getRateLimitPerSecond());

        int page = 0;
        int sentInThisSession = 0;

        while (true) {
            PageResult<CampaignAudienceMember> batch = campaignRepository.findCampaignAudience(
                    campaign.getCompanyId(), campaign.getId(), page, properties.getBatchSize());
            
            if (batch.content() == null || batch.content().isEmpty()) break;

            for (CampaignAudienceMember member : batch.content()) {
                boolean isSubscribed = member.marketingStatus() == MarketingStatus.SUBSCRIBED;
                boolean shouldSend;

                // 4. Strict explicit evaluation for Relaunch vs Normal
                if (isRelaunch) {
                    shouldSend = member.sendStatus() == DeliveryStatus.NOT_SENT
                              || member.sendStatus() == DeliveryStatus.FAILED;
                } else {
                    shouldSend = member.sendStatus() == null
                              || member.sendStatus() == DeliveryStatus.NOT_SENT;
                }

                if (!isSubscribed || !shouldSend) continue;

                // Pause thread accurately without blocking CPU (Replaces Thread.sleep)
                rateLimiter.acquire();

                try {
                    boolean success = processSingleCustomer(campaign, tpl, member);
                    if (success) {
                        sentInThisSession++;
                    }
                } catch (DataIntegrityViolationException e) {
                    // 5. DB-Level Idempotency Guard triggers here if log exists
                    log.warn("DB Idempotency: Duplicate log prevented for campaign {} and customer {}", campaign.getId(), member.customerId());
                } catch (Exception e) {
                    log.error("Unexpected error processing customer {}: {}", member.customerId(), e.getMessage());
                }
            }
            
            page++;
            if (page >= batch.totalPages()) break;
        }

        // 6. Atomic Completion
        campaignRepository.completeCampaign(campaignId, sentInThisSession);
        log.info("Campaign {} execution finished. Emails sent this session: {}", campaignId, sentInThisSession);
    }

    private boolean processSingleCustomer(MarketingCampaign campaign, MarketingTemplate tpl, CampaignAudienceMember member) {
        CampaignLog logEntry = CampaignLog.createPending(campaign.getId(), campaign.getCompanyId(), member.customerId(), tpl.getId());
        
        // This save triggers the UNIQUE constraints (DataIntegrityViolationException) if already sent
        campaignRepository.saveLog(logEntry);

        int attempts = 0;
        int maxRetries = properties.getMaxRetries();
        
        while (attempts <= maxRetries) {
            try {
                sendEmailToCustomer(campaign.getId(), logEntry.getId(), tpl, member, logEntry);
                campaignRepository.saveLog(logEntry);
                return true;
            } catch (Exception e) {
                attempts++;
                log.warn("Failed sending to {} (attempt {}): {}", member.email(), attempts, e.getMessage());
                if (attempts > maxRetries) {
                    logEntry.markFailed(e.getMessage());
                    campaignRepository.saveLog(logEntry);
                    return false;
                }
                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 1000L); // Backoff is okay for brief I/O retry delays
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void sendEmailToCustomer(Long campaignId, Long logId, MarketingTemplate tpl, CampaignAudienceMember member, CampaignLog logEntry) {
        String unsubscribeLink = properties.getPublicBaseUrl() + "/api/v1/public/marketing/preferences?token=" + member.unsubscribeToken();
        Map<String, String> vars = Map.of(
                "name", member.name() != null ? member.name() : "Customer",
                "unsubscribe_link", unsubscribeLink);

        String body = tpl.compile(vars);
        body = rewriteLinksForTracking(logId, body);

        String pixelUrl = properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/open/" + logId + "/pixel.gif";
        body += "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";

        EmailMessage msg = EmailMessage.of(member.email(), tpl.compileSubject(vars), body, member.unsubscribeToken());
        String messageId = mailingSystem.send(msg);
        
        logEntry.markSent(messageId);
    }

    private String rewriteLinksForTracking(Long logId, String html) {
        Pattern pattern = Pattern.compile("href=\"(https?://[^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        long expiresAt = Instant.now().plus(properties.getLinkExpirationHours(), ChronoUnit.HOURS).toEpochMilli();

        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            if (originalUrl.contains("/marketing/preferences") || originalUrl.contains("/marketing/track/click/")) {
                matcher.appendReplacement(sb, "href=\"" + originalUrl + "\"");
                continue;
            }

            String payload = originalUrl + "|" + expiresAt;
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
            String signature = generateHmac(encodedPayload);
            String trackUrl = String.format("%s/api/v1/public/marketing/track/click/%d?p=%s&sig=%s",
                    properties.getPublicBaseUrl(), logId, encodedPayload, signature);

            matcher.appendReplacement(sb, "href=\"" + trackUrl + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String generateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getHmacSecret().getBytes(), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("HMAC generation failed", e);
        }
    }
}