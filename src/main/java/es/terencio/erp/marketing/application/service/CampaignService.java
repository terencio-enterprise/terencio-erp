package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignResponse;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CreateCampaignRequest;
import es.terencio.erp.marketing.application.port.in.CampaignTrackingUseCase;
import es.terencio.erp.marketing.application.port.in.ManageCampaignsUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort.MarketingCustomer;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.CampaignStatus;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class CampaignService implements ManageCampaignsUseCase, CampaignTrackingUseCase {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    private static final byte[] PIXEL_BYTES = Base64.getDecoder()
            .decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

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
        MarketingCampaign campaign = new MarketingCampaign(
                null, companyId, request.name(), request.templateId(), CampaignStatus.DRAFT);

        MarketingCampaign saved = campaignRepository.saveCampaign(campaign);
        return toCampaignDto(saved);
    }

    @Override
    public CampaignResponse getCampaign(Long campaignId) {
        return campaignRepository.findCampaignById(campaignId)
                .map(this::toCampaignDto)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
    }

    @Override
    @Async
    public void launchCampaign(Long campaignId) {
        executeCampaign(campaignId, false);
    }

    @Override
    @Async
    public void relaunchCampaign(Long campaignId) {
        executeCampaign(campaignId, true);
    }

    private void executeCampaign(Long campaignId, boolean isRelaunch) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found"));

        if (campaign.getStatus() == CampaignStatus.SENDING && !isRelaunch) {
            log.warn("Campaign {} is already sending. Aborting execution.", campaignId);
            return;
        }

        campaign.setStatus(CampaignStatus.SENDING);
        campaignRepository.saveCampaign(campaign);

        MarketingTemplate tpl = campaignRepository.findTemplateById(campaign.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        int offset = 0;
        int sentInThisSession = 0;
        long delay = 1000 / Math.max(1, properties.getRateLimitPerSecond());

        while (true) {
            List<MarketingCustomer> batch = customerPort.findAudience(null, properties.getBatchSize(), offset);
            if (batch.isEmpty())
                break;

            for (MarketingCustomer customer : batch) {
                // Skip if already emailed in this campaign (Idempotency)
                if (!customer.canReceiveMarketing() || campaignRepository.hasLog(campaignId, customer.id()))
                    continue;

                processSingleCustomer(campaign, tpl, customer);
                sentInThisSession++;

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            offset += properties.getBatchSize();
        }

        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setMetricsSent(campaign.getMetricsSent() + sentInThisSession);
        campaignRepository.saveCampaign(campaign);
    }

    private void processSingleCustomer(MarketingCampaign campaign, MarketingTemplate tpl, MarketingCustomer customer) {
        CampaignLog logEntry = new CampaignLog();
        logEntry.setCampaignId(campaign.getId());
        logEntry.setCompanyId(campaign.getCompanyId());
        logEntry.setCustomerId(customer.id());
        logEntry.setTemplateId(tpl.getId());
        logEntry.setStatus(DeliveryStatus.PENDING);
        logEntry.setSentAt(Instant.now());

        campaignRepository.saveLog(logEntry);

        try {
            sendEmailToCustomer(campaign.getId(), logEntry.getId(), tpl, customer, logEntry);
            logEntry.setStatus(DeliveryStatus.SENT);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", customer.email(), e.getMessage());
            logEntry.setStatus(DeliveryStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
        }
        campaignRepository.saveLog(logEntry);
    }

    private void sendEmailToCustomer(Long campaignId, Long logId, MarketingTemplate tpl, MarketingCustomer customer,
            CampaignLog logEntry) {
        String unsubscribeLink = properties.getPublicBaseUrl() + "/api/v1/public/marketing/preferences?token="
                + customer.unsubscribeToken();
        Map<String, String> vars = Map.of(
                "name", customer.name() != null ? customer.name() : "Customer",
                "unsubscribe_link", unsubscribeLink);

        String body = tpl.compile(vars);
        body = rewriteLinksForTracking(logId, body);

        // Append invisible tracking pixel
        String pixelUrl = properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/open/" + logId + "/pixel.gif";
        body += "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";

        EmailMessage msg = EmailMessage.of(customer.email(), tpl.compileSubject(vars), body,
                customer.unsubscribeToken());
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
            if (originalUrl.contains("/marketing/preferences")) {
                matcher.appendReplacement(sb, "href=\"" + originalUrl + "\""); // Don't track preference link
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public byte[] registerOpenAndGetPixel(Long logId) {
        campaignRepository.findLogById(logId).ifPresent(entry -> {
            if (entry.getOpenedAt() == null) {
                entry.setOpenedAt(Instant.now());
                entry.setStatus(DeliveryStatus.OPENED);
                campaignRepository.saveLog(entry);
                campaignRepository.incrementCampaignMetric(entry.getCampaignId(), "opened");
            }
        });
        return PIXEL_BYTES;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String registerClickAndGetRedirectUrl(Long logId, String encodedPayload, String signature) {
        if (!generateHmac(encodedPayload).equals(signature)) {
            log.error("Invalid click signature for log: {}", logId);
            return properties.getPublicBaseUrl();
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encodedPayload));
            String[] parts = decoded.split("\\|");
            String originalUrl = parts[0];
            long expiresAt = Long.parseLong(parts[1]);

            if (Instant.now().toEpochMilli() > expiresAt) {
                log.warn("Expired tracking link: {}", logId);
                return originalUrl; // Redirect anyway, but log it
            }

            campaignRepository.findLogById(logId).ifPresent(entry -> {
                if (entry.getClickedAt() == null) {
                    entry.setClickedAt(Instant.now());
                    campaignRepository.saveLog(entry);
                    campaignRepository.incrementCampaignMetric(entry.getCampaignId(), "clicked");
                }
            });
            return originalUrl;
        } catch (Exception e) {
            return properties.getPublicBaseUrl();
        }
    }

    private CampaignResponse toCampaignDto(MarketingCampaign c) {
        return new CampaignResponse(c.getId(), c.getName(), c.getStatus(), c.getScheduledAt(),
                c.getMetricsTotalRecipients(), c.getMetricsSent(), c.getMetricsOpened(),
                c.getMetricsClicked(), c.getMetricsBounced());
    }

    // --- Unused Boilerplate Omitted for this preview ---
    @Override
    public List<CampaignAudienceMember> getCampaignAudience(Long campaignId) {
        return List.of();
    }

    @Override
    public void scheduleCampaign(Long campaignId, Instant s) {
    }

    @Override
    public void dryRun(Long t, String e) {
    }
}