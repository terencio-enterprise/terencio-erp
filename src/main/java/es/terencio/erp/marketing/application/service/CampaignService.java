package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
import es.terencio.erp.marketing.domain.model.*;
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

    // --- Create, Get, Schedule Omitted for Brevity (Same as before) ---
    @Override @Transactional public CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request) { return null; }
    @Override public CampaignResponse getCampaign(Long campaignId) { return null; }
    @Override public List<CampaignAudienceMember> getCampaignAudience(Long campaignId) { return List.of(); }
    @Override @Transactional public void scheduleCampaign(Long campaignId, Instant scheduledAt) { }
    @Override public void dryRun(Long templateId, String testEmail) { }
    
    @Override
    @Async
    public void launchCampaign(Long campaignId) { executeCampaign(campaignId, false); }

    @Override
    @Async
    public void relaunchCampaign(Long campaignId) { executeCampaign(campaignId, true); }

    private void executeCampaign(Long campaignId, boolean isRelaunch) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId).orElseThrow();
        campaign.setStatus(CampaignStatus.SENDING);
        campaignRepository.saveCampaign(campaign);
        
        MarketingTemplate tpl = campaignRepository.findTemplateById(campaign.getTemplateId()).orElseThrow();
        
        int offset = 0;
        int sentCount = 0;
        long rateLimitDelayMs = 1000 / Math.max(1, properties.getRateLimitPerSecond());
        boolean hasMore = true;

        while (hasMore) {
            List<MarketingCustomer> batch = customerPort.findAudience(null, properties.getBatchSize(), offset);
            if (batch.isEmpty()) break;

            for (MarketingCustomer customer : batch) {
                if (!customer.canReceiveMarketing() || campaignRepository.hasLog(campaignId, customer.id())) continue;

                CampaignLog logEntry = campaignRepository.saveLog(
                    new CampaignLog(null, campaignId, customer.companyId(), customer.id(), tpl.getId(), Instant.now(), null, DeliveryStatus.PENDING, null, null, null, null)
                );

                int attempts = 0;
                boolean success = false;
                while (attempts < properties.getMaxRetries() && !success) {
                    try {
                        sendEmailToCustomer(campaignId, logEntry.getId(), tpl, customer, logEntry);
                        logEntry.setStatus(DeliveryStatus.SENT);
                        success = true;
                        sentCount++;
                        Thread.sleep(rateLimitDelayMs);
                    } catch (Exception e) {
                        attempts++;
                        log.warn("Attempt {} failed for customer {}: {}", attempts, customer.email(), e.getMessage());
                        if (attempts >= properties.getMaxRetries()) {
                            logEntry.setStatus(DeliveryStatus.FAILED);
                            logEntry.setErrorMessage(e.getMessage());
                        } else {
                            try { Thread.sleep((long) Math.pow(2, attempts) * 1000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        }
                    }
                }
                campaignRepository.saveLog(logEntry);
            }
            offset += properties.getBatchSize();
        }
        
        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setMetricsSent(campaign.getMetricsSent() + sentCount);
        campaignRepository.saveCampaign(campaign);
    }

    private void sendEmailToCustomer(Long campaignId, Long logId, MarketingTemplate tpl, MarketingCustomer customer, CampaignLog logEntry) {
        String unsubscribeLink = properties.getPublicBaseUrl() + "/v1/public/marketing/preferences?token=" + customer.unsubscribeToken();
        String body = tpl.compile(Map.of("name", customer.name() != null ? customer.name() : "Valued Customer", "unsubscribe_link", unsubscribeLink));
        
        body = rewriteLinksForTracking(logId, body);
        String pixelUrl = properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/open/" + logId + "/pixel.gif";
        body += "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";

        EmailMessage msg = EmailMessage.of(customer.email(), "Campaign", body, customer.unsubscribeToken());
        String messageId = mailingSystem.send(msg); 
        logEntry.setMessageId(messageId); 
    }

    private String rewriteLinksForTracking(Long logId, String html) {
        Pattern pattern = Pattern.compile("href=\"(http[s]?://[^\"]+)\"");
        Matcher matcher = pattern.matcher(html);
        StringBuilder sb = new StringBuilder();
        
        long expiresAt = Instant.now().plus(properties.getLinkExpirationHours(), ChronoUnit.HOURS).toEpochMilli();
        
        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            String payload = originalUrl + "|" + expiresAt;
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
            String signature = generateHmac(encodedPayload);
            String newHref = "href=\"" + properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/click/" + logId + "?p=" + encodedPayload + "&sig=" + signature + "\"";
            matcher.appendReplacement(sb, newHref);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String generateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getHmacSecret().getBytes(), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public byte[] registerOpenAndGetPixel(Long logId) {
        campaignRepository.findLogById(logId).ifPresent(logEntry -> {
            if (logEntry.getOpenedAt() == null) {
                logEntry.setOpenedAt(Instant.now());
                logEntry.setStatus(DeliveryStatus.OPENED);
                campaignRepository.saveLog(logEntry);
                campaignRepository.incrementCampaignMetric(logEntry.getCampaignId(), "opened");
            }
        });
        return PIXEL_BYTES;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String registerClickAndGetRedirectUrl(Long logId, String encodedPayload, String signature) {
        if (!generateHmac(encodedPayload).equals(signature)) {
            log.warn("🚨 SECURITY: Invalid HMAC signature for click tracking logId: {}", logId);
            return properties.getPublicBaseUrl(); 
        }

        String decodedPayload = new String(Base64.getUrlDecoder().decode(encodedPayload));
        String[] parts = decodedPayload.split("\\|");
        String originalUrl = parts[0];
        long expiresAt = Long.parseLong(parts[1]);

        if (Instant.now().toEpochMilli() > expiresAt) {
            log.warn("🚨 SECURITY: Expired tracking link clicked for logId: {}", logId);
            return properties.getPublicBaseUrl();
        }

        campaignRepository.findLogById(logId).ifPresent(logEntry -> {
            if (logEntry.getClickedAt() == null) {
                logEntry.setClickedAt(Instant.now());
                campaignRepository.saveLog(logEntry);
                campaignRepository.incrementCampaignMetric(logEntry.getCampaignId(), "clicked");
            }
        });
        return originalUrl;
    }
}
