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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

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
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class CampaignService implements ManageCampaignsUseCase, CampaignTrackingUseCase {
    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);
    private static final byte[] PIXEL_BYTES = Base64.getDecoder()
            .decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");

    private final CampaignRepositoryPort campaignRepository;
    private final CustomerIntegrationPort customerPort;
    private final MailingSystemPort mailingSystem;
    private final MarketingProperties properties;
    private final ObjectMapper objectMapper;

    public CampaignService(CampaignRepositoryPort campaignRepository, CustomerIntegrationPort customerPort,
            MailingSystemPort mailingSystem, MarketingProperties properties, ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.customerPort = customerPort;
        this.mailingSystem = mailingSystem;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    private MarketingCampaign getCampaignEntity(UUID companyId, Long campaignId) {
        MarketingCampaign campaign = campaignRepository.findCampaignById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        if (!campaign.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Campaign not found");
        }
        return campaign;
    }

    @Override
    @Transactional
    public CampaignResponse createDraft(UUID companyId, CreateCampaignRequest request) {
        String filterJson = null;
        try {
            if (request.audienceFilter() != null) {
                filterJson = objectMapper.writeValueAsString(request.audienceFilter());
            }
        } catch (Exception e) { log.warn("Failed to serialize audience filter", e); }

        MarketingCampaign campaign = MarketingCampaign.createDraft(companyId, request.name(), request.templateId(), filterJson);
        MarketingCampaign saved = campaignRepository.saveCampaign(campaign);
        return toCampaignDto(saved);
    }

    @Override
    @Transactional
    public CampaignResponse updateDraft(UUID companyId, Long campaignId, CreateCampaignRequest request) {
        MarketingCampaign campaign = getCampaignEntity(companyId, campaignId);
        String filterJson = null;
        try {
            if (request.audienceFilter() != null) {
                filterJson = objectMapper.writeValueAsString(request.audienceFilter());
            }
        } catch (Exception e) { log.warn("Failed to serialize audience filter", e); }
        
        campaign.updateDraft(request.name(), request.templateId(), filterJson);
        return toCampaignDto(campaignRepository.saveCampaign(campaign));
    }

    @Override
    public CampaignResponse getCampaign(UUID companyId, Long campaignId) {
        return toCampaignDto(getCampaignEntity(companyId, campaignId));
    }

    @Override
    public List<CampaignAudienceMember> getCampaignAudience(UUID companyId, Long campaignId, int limit, int page) {
        List<MarketingCustomer> batch = customerPort.findAudience(campaignId, limit, page);
        return batch.stream()
            .map(c -> new CampaignAudienceMember(c.id(), c.email(), c.name(), c.canReceiveMarketing() ? "SUBSCRIBED" : "UNSUBSCRIBED"))
            .collect(Collectors.toList());
    }

    @Override
    @Async
    public void launchCampaign(UUID companyId, Long campaignId) {
        executeCampaign(companyId, campaignId, false);
    }

    @Override
    @Async
    public void relaunchCampaign(UUID companyId, Long campaignId) {
        executeCampaign(companyId, campaignId, true);
    }

    private void executeCampaign(UUID companyId, Long campaignId, boolean isRelaunch) {
        MarketingCampaign campaign;
        try {
            campaign = getCampaignEntity(companyId, campaignId);
        } catch(Exception e) { log.error("Abort launch, campaign not found: {}", campaignId); return; }

        if (campaign.getStatus() == CampaignStatus.SENDING && !isRelaunch) {
            log.warn("Campaign {} is already sending. Aborting execution.", campaignId);
            return;
        }
        
        if (campaign.getStatus() != CampaignStatus.DRAFT && campaign.getStatus() != CampaignStatus.SCHEDULED && !isRelaunch) {
            log.warn("Campaign {} is not in a valid state to launch.", campaignId);
            return;
        }

        campaign.startSending();
        campaignRepository.saveCampaign(campaign);

        MarketingTemplate tpl = campaignRepository.findTemplateById(campaign.getTemplateId())
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        int offset = 0;
        int sentInThisSession = 0;
        long delay = 1000 / Math.max(1, properties.getRateLimitPerSecond());

        while (true) {
            List<MarketingCustomer> batch = customerPort.findAudience(campaignId, properties.getBatchSize(), offset);
            if (batch.isEmpty()) break;

            for (MarketingCustomer customer : batch) {
                if (!customer.canReceiveMarketing() || campaignRepository.hasLog(campaignId, customer.id()))
                    continue;

                boolean success = processSingleCustomer(campaign, tpl, customer);
                if (success) {
                    sentInThisSession++;
                }

                try { Thread.sleep(delay); } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            offset += properties.getBatchSize();
        }

        campaign.complete();
        campaign.addSent(sentInThisSession);
        campaignRepository.saveCampaign(campaign);
    }

    private boolean processSingleCustomer(MarketingCampaign campaign, MarketingTemplate tpl, MarketingCustomer customer) {
        CampaignLog logEntry = CampaignLog.createPending(campaign.getId(), campaign.getCompanyId(), customer.id(), tpl.getId());
        campaignRepository.saveLog(logEntry);

        int attempts = 0;
        int maxRetries = properties.getMaxRetries();
        
        while (attempts <= maxRetries) {
            try {
                sendEmailToCustomer(campaign.getId(), logEntry.getId(), tpl, customer, logEntry);
                campaignRepository.saveLog(logEntry);
                return true;
            } catch (Exception e) {
                attempts++;
                log.warn("Failed sending to {} (attempt {}): {}", customer.email(), attempts, e.getMessage());
                if (attempts > maxRetries) {
                    logEntry.markFailed(e.getMessage());
                    campaignRepository.saveLog(logEntry);
                    return false;
                }
                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 1000L); // simple exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void sendEmailToCustomer(Long campaignId, Long logId, MarketingTemplate tpl, MarketingCustomer customer, CampaignLog logEntry) {
        String unsubscribeLink = properties.getPublicBaseUrl() + "/api/v1/public/marketing/preferences?token=" + customer.unsubscribeToken();
        Map<String, String> vars = Map.of(
                "name", customer.name() != null ? customer.name() : "Customer",
                "unsubscribe_link", unsubscribeLink);

        String body = tpl.compile(vars);
        body = rewriteLinksForTracking(logId, body);

        String pixelUrl = properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/open/" + logId + "/pixel.gif";
        body += "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";

        EmailMessage msg = EmailMessage.of(customer.email(), tpl.compileSubject(vars), body, customer.unsubscribeToken());
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

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public byte[] registerOpenAndGetPixel(Long logId) {
        campaignRepository.findLogById(logId).ifPresent(entry -> {
            if (entry.markOpened()) {
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
            
            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                log.warn("Invalid scheme on redirect: {}", originalUrl);
                return properties.getPublicBaseUrl();
            }

            if (Instant.now().toEpochMilli() > expiresAt) {
                log.warn("Expired tracking link: {}", logId);
                return originalUrl;
            }

            campaignRepository.findLogById(logId).ifPresent(entry -> {
                if (entry.markClicked()) {
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
                c.getTotalRecipients(), c.getSent(), c.getDelivered(), c.getOpened(),
                c.getClicked(), c.getBounced(), c.getUnsubscribed());
    }

    @Override
    @Transactional
    public void scheduleCampaign(UUID companyId, Long campaignId, Instant s) {
        MarketingCampaign campaign = getCampaignEntity(companyId, campaignId);
        campaign.schedule(s);
        campaignRepository.saveCampaign(campaign);
    }
    
    @Override
    @Transactional
    public void cancelCampaign(UUID companyId, Long campaignId) {
        MarketingCampaign campaign = getCampaignEntity(companyId, campaignId);
        campaign.cancel();
        campaignRepository.saveCampaign(campaign);
    }

    @Override
    public void dryRun(UUID companyId, Long templateId, String testEmail) {
        MarketingTemplate tpl = campaignRepository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));
                
        if (!tpl.getCompanyId().equals(companyId)) {
            throw new InvariantViolationException("Template access denied");
        }
        
        Map<String, String> vars = Map.of(
                "name", "Jane Doe",
                "unsubscribe_link", properties.getPublicBaseUrl() + "/api/v1/public/marketing/preferences?token=test-token"
        );
        String body = tpl.compile(vars);
        EmailMessage msg = EmailMessage.of(testEmail, tpl.compileSubject(vars), body, "test-token");
        mailingSystem.send(msg);
    }
}