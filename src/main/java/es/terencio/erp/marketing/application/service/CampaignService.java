package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import es.terencio.erp.marketing.application.dto.MarketingDtos.*;
import es.terencio.erp.marketing.application.port.in.ManageCampaignsUseCase;
import es.terencio.erp.marketing.application.port.in.CampaignTrackingUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort.MarketingCustomer;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.marketing.domain.model.CampaignStatus;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;

public class CampaignService implements ManageCampaignsUseCase, CampaignTrackingUseCase {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    private static final byte[] PIXEL_BYTES = Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private final CampaignRepositoryPort campaignRepository;
    private final CustomerIntegrationPort customerPort;
    private final MailingSystemPort mailingSystem;
    private final MarketingProperties properties;

    public CampaignService(CampaignRepositoryPort campaignRepository, CustomerIntegrationPort customerPort, 
                           MailingSystemPort mailingSystem, MarketingProperties properties) {
        this.campaignRepository = campaignRepository;
        this.customerPort = customerPort;
        this.mailingSystem = mailingSystem;
        this.properties = properties;
    }

    @Override
    @Transactional
    public CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request) {
        MarketingCampaign campaign = new MarketingCampaign(null, companyId, request.name(), request.templateId(), CampaignStatus.DRAFT);
        campaign = campaignRepository.saveCampaign(campaign);
        return toResponse(campaign);
    }

    @Override
    public CampaignResponse getCampaign(Long campaignId) {
        MarketingCampaign c = campaignRepository.findCampaignById(campaignId).orElseThrow();
        return toResponse(c);
    }

    @Override
    public List<CampaignAudienceMember> getCampaignAudience(Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId).orElseThrow();
        // Fetching top 1000 for preview purposes
        List<MarketingCustomer> audience = customerPort.findAudience(null, 1000, 0); 
        return audience.stream().map(c -> {
            boolean alreadySent = campaignRepository.hasLog(campaignId, c.id());
            return new CampaignAudienceMember(c.id(), c.email(), c.name(), alreadySent ? "SENT" : "PENDING");
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void scheduleCampaign(Long campaignId, Instant scheduledAt) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId).orElseThrow();
        campaign.setStatus(CampaignStatus.SCHEDULED);
        campaign.setScheduledAt(scheduledAt);
        campaignRepository.saveCampaign(campaign);
    }

    @Override
    @Async // Runs in background thread, freeing HTTP worker
    public void launchCampaign(Long campaignId) {
        executeCampaign(campaignId, false);
    }

    @Override
    @Async
    public void relaunchCampaign(Long campaignId) {
        executeCampaign(campaignId, true);
    }

    // Notice: Removed @Transactional from this method to prevent long-running locks on the DB.
    // We isolate transactions to individual batches or single saves.
    private void executeCampaign(Long campaignId, boolean isRelaunch) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId).orElseThrow();
        if (!isRelaunch && campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.SCHEDULED) {
            log.error("Campaign {} cannot be launched from status: {}", campaignId, campaign.getStatus());
            return;
        }
        
        campaign.setStatus(CampaignStatus.SENDING);
        campaignRepository.saveCampaign(campaign);
        
        MarketingTemplate tpl = campaignRepository.findTemplateById(campaign.getTemplateId()).orElseThrow();
        
        int offset = 0;
        int sentCount = 0;
        long rateLimitDelayMs = 1000 / Math.max(1, properties.getRateLimitPerSecond());
        
        boolean hasMore = true;
        
        log.info("Starting execution for Campaign ID: {} with Batch Size: {}", campaignId, properties.getBatchSize());

        while (hasMore) {
            // Paginated fetching avoids OOM exceptions for huge audiences
            List<MarketingCustomer> batch = customerPort.findAudience(null, properties.getBatchSize(), offset);
            if (batch.isEmpty()) {
                hasMore = false;
                break;
            }

            for (MarketingCustomer customer : batch) {
                if (!customer.canReceiveMarketing()) continue;
                if (campaignRepository.hasLog(campaignId, customer.id())) continue;

                // Create Pending Log
                CampaignLog logEntry = campaignRepository.saveLog(
                    new CampaignLog(null, campaignId, customer.companyId(), customer.id(), tpl.getId(), Instant.now(), DeliveryStatus.PENDING, null, null)
                );

                try {
                    sendEmailToCustomer(campaignId, logEntry.getId(), tpl, customer, logEntry);
                    logEntry.setStatus(DeliveryStatus.SENT);
                    sentCount++;
                    
                    // Simple Rate Limiting Strategy
                    Thread.sleep(rateLimitDelayMs);
                } catch (Exception e) {
                    log.error("Failed to send to {}", customer.email(), e);
                    logEntry.setStatus(DeliveryStatus.FAILED);
                    logEntry.setErrorMessage(e.getMessage());
                }
                
                // Save updated state
                campaignRepository.saveLog(logEntry);
            }
            offset += properties.getBatchSize();
        }
        
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setMetricsSent(campaign.getMetricsSent() + sentCount);
        campaignRepository.saveCampaign(campaign);
        log.info("Campaign ID: {} Completed. Total sent this run: {}", campaignId, sentCount);
    }

    private void sendEmailToCustomer(Long campaignId, Long logId, MarketingTemplate tpl, MarketingCustomer customer, CampaignLog logEntry) {
        String unsubscribeLink = properties.getPublicBaseUrl() + "/v1/public/marketing/preferences?token=" + customer.unsubscribeToken();
        String body = tpl.compile(Map.of("name", customer.name() != null ? customer.name() : "Valued Customer", "unsubscribe_link", unsubscribeLink));
        String subject = tpl.compileSubject(Map.of("name", customer.name() != null ? customer.name() : "Valued Customer"));
        
        body = rewriteLinksForTracking(logId, body);
        String pixelUrl = properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/open/" + logId + "/pixel.gif";
        body += "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";

        EmailMessage msg = EmailMessage.of(customer.email(), subject, body, customer.unsubscribeToken());
        String messageId = mailingSystem.send(msg); 
        logEntry.setMessageId(messageId); 
    }

    private String rewriteLinksForTracking(Long logId, String html) {
        Pattern pattern = Pattern.compile("href=\"(http[s]?://[^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            String encodedUrl = Base64.getUrlEncoder().withoutPadding().encodeToString(originalUrl.getBytes());
            String signature = generateHmac(encodedUrl);
            String newHref = "href=\"" + properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/click/" + logId + "?u=" + encodedUrl + "&sig=" + signature + "\"";
            matcher.appendReplacement(sb, newHref);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String generateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(properties.getHmacSecret().getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error signing link", e);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public byte[] registerOpenAndGetPixel(Long logId) {
        campaignRepository.findLogById(logId).ifPresent(log -> {
            if (log.getOpenedAt() == null) {
                log.setOpenedAt(Instant.now());
                log.setStatus(DeliveryStatus.OPENED);
                campaignRepository.saveLog(log);
                campaignRepository.incrementCampaignMetric(log.getCampaignId(), "opened");
            }
        });
        return PIXEL_BYTES;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String registerClickAndGetRedirectUrl(Long logId, String encodedUrl, String signature) {
        if (!generateHmac(encodedUrl).equals(signature)) {
            log.warn("Invalid HMAC signature for link tracking logId: {}", logId);
            return properties.getPublicBaseUrl(); 
        }

        String originalUrl = new String(Base64.getUrlDecoder().decode(encodedUrl));
        campaignRepository.findLogById(logId).ifPresent(log -> {
            if (log.getClickedAt() == null) {
                log.setClickedAt(Instant.now());
                campaignRepository.saveLog(log);
                campaignRepository.incrementCampaignMetric(log.getCampaignId(), "clicked");
            }
        });
        return originalUrl;
    }

    @Override
    public void dryRun(Long templateId, String testEmail) {
        MarketingTemplate tpl = campaignRepository.findTemplateById(templateId).orElseThrow();
        String body = tpl.compile(Map.of("name", "Test User", "unsubscribe_link", "#"));
        EmailMessage msg = EmailMessage.of(testEmail, "[DRY RUN] " + tpl.getSubjectTemplate(), body, "dry-run-token");
        mailingSystem.send(msg);
    }
    
    private CampaignResponse toResponse(MarketingCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getStatus(), c.getScheduledAt(), 
            c.getMetricsTotalRecipients(), c.getMetricsSent(), c.getMetricsOpened(), c.getMetricsClicked(), c.getMetricsBounced());
    }
}
