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
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;

/**
 * Handles incoming webhooks from AWS SNS/SES for email delivery events.
 */
public class WebhookService implements ProcessWebhookUseCase {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final CampaignRepositoryPort repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public WebhookService(CampaignRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void processSesEvent(String payload) {
        String messageType = extractField(payload, "\"Type\"\\s*:\\s*\"([^\"]+)\"");
        if ("SubscriptionConfirmation".equals(messageType)) {
            String subscribeUrl = extractField(payload, "\"SubscribeURL\"\\s*:\\s*\"([^\"]+)\"");
            if (subscribeUrl != null) {
                log.info("Auto-confirming SNS Subscription...");
                restTemplate.getForEntity(subscribeUrl, String.class);
            }
            return;
        }

        String messageId = extractField(payload, "\"messageId\"\\s*:\\s*\"([^\"]+)\"");
        String notificationType = extractField(payload, "\"notificationType\"\\s*:\\s*\"([^\"]+)\"");
        String email = extractField(payload, "\"destination\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");

        if (messageId == null || notificationType == null)
            return;

        EmailDeliveryEvent event = new EmailDeliveryEvent(null, messageId, email, notificationType, null, null, payload,
                Instant.now());
        repository.saveDeliveryEvent(event);

        repository.findLogByMessageId(messageId).ifPresent(logEntry -> {
            updateLogEntry(logEntry, notificationType);
            repository.saveLog(logEntry);
        });
    }

    private void updateLogEntry(CampaignLog logEntry, String type) {
        switch (type.toUpperCase()) {
            case "DELIVERY" -> {
                if (logEntry.markDelivered()) {
                    repository.incrementCampaignMetric(logEntry.getCampaignId(), "delivered");
                }
            }
            case "BOUNCE" -> {
                if (logEntry.markBounced()) {
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "BOUNCED");
                    repository.incrementCampaignMetric(logEntry.getCampaignId(), "bounced");
                }
            }
            case "COMPLAINT" -> {
                if (logEntry.markComplained()) {
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "COMPLAINED");
                }
            }
        }
    }

    private String extractField(String payload, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }
}