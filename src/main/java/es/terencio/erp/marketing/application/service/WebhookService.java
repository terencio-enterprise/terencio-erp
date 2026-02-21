package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import es.terencio.erp.marketing.application.port.in.ProcessWebhookUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;

public class WebhookService implements ProcessWebhookUseCase {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final CampaignRepositoryPort repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WebhookService(CampaignRepositoryPort repository, MarketingProperties properties) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void processSesEvent(String payload) {
        // 🔥 CRITICAL: Handle SNS Subscription Confirmation automatically
        String messageType = extractField(payload, "\"Type\"\\s*:\\s*\"([^\"]+)\"");
        if ("SubscriptionConfirmation".equals(messageType)) {
            String subscribeUrl = extractField(payload, "\"SubscribeURL\"\\s*:\\s*\"([^\"]+)\"");
            if (subscribeUrl != null) {
                log.info("Auto-confirming SNS Webhook Subscription...");
                restTemplate.getForEntity(subscribeUrl, String.class);
            }
            return;
        }

        // Parse Standard Notification
        String messageId = extractField(payload, "\"messageId\"\\s*:\\s*\"([^\"]+)\"");
        String notificationType = extractField(payload, "\"notificationType\"\\s*:\\s*\"([^\"]+)\"");
        String email = extractField(payload, "\"destination\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");
        
        if (messageId == null || notificationType == null) return;

        EmailDeliveryEvent event = new EmailDeliveryEvent(null, messageId, email, notificationType, null, null, payload, null);
        repository.saveDeliveryEvent(event);

        repository.findLogByMessageId(messageId).ifPresent(logEntry -> {
            if ("Delivery".equalsIgnoreCase(notificationType)) {
                if (logEntry.getDeliveredAt() == null) {
                    logEntry.setDeliveredAt(Instant.now());
                    logEntry.setStatus(DeliveryStatus.DELIVERED);
                    repository.incrementCampaignMetric(logEntry.getCampaignId(), "delivered");
                }
            } else if ("Bounce".equalsIgnoreCase(notificationType)) {
                if (logEntry.getStatus() != DeliveryStatus.BOUNCED) {
                    logEntry.setStatus(DeliveryStatus.BOUNCED);
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "BOUNCED");
                    repository.incrementCampaignMetric(logEntry.getCampaignId(), "bounced");
                }
            } else if ("Complaint".equalsIgnoreCase(notificationType)) {
                if (logEntry.getStatus() != DeliveryStatus.COMPLAINED) {
                    logEntry.setStatus(DeliveryStatus.COMPLAINED);
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "COMPLAINED");
                }
            }
            repository.saveLog(logEntry);
        });
    }

    private String extractField(String payload, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }
}
