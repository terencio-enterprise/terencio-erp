package es.terencio.erp.marketing.application.service.webhook;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.marketing.application.port.in.WebhookProcessingUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;

public class SesWebhookService implements WebhookProcessingUseCase {
    private static final Logger log = LoggerFactory.getLogger(SesWebhookService.class);
    
    private static final String METRIC_DELIVERED = "delivered";
    private static final String METRIC_BOUNCED = "bounced";
    
    private final CampaignRepositoryPort repository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public SesWebhookService(CampaignRepositoryPort repository, ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    @Transactional
    public void processSesEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String type = root.path("Type").asText(null);

            if ("SubscriptionConfirmation".equals(type)) {
                String subscribeUrl = root.path("SubscribeURL").asText(null);
                if (subscribeUrl != null) {
                    log.info("Auto-confirming SNS Subscription...");
                    restTemplate.getForEntity(subscribeUrl, String.class);
                }
                return;
            }

            final JsonNode eventNode = ("Notification".equals(type) && root.has("Message"))
                    ? objectMapper.readTree(root.path("Message").asText())
                    : root;

            final String messageId = eventNode.path("mail").path("messageId").asText(
                    eventNode.path("messageId").asText(null)
            );

            final String eventType = eventNode.path("notificationType").asText(
                    eventNode.path("eventType").asText(null)
            );

            JsonNode mailNode = eventNode.has("mail") ? eventNode.get("mail") : eventNode;
            String email = mailNode.path("destination").path(0).asText(null);

            if (messageId == null || eventType == null) return;

            String bounceType = eventNode.path("bounce").path("bounceType").asText(null);
            String bounceSubtype = eventNode.path("bounce").path("bounceSubType").asText(null);

            EmailDeliveryEvent event = new EmailDeliveryEvent(null, messageId, email, eventType, bounceType, bounceSubtype, payload,
                    Instant.now());
            repository.saveDeliveryEvent(event);

            repository.findLogByMessageId(messageId).ifPresent(logEntry -> {
                updateLogEntry(logEntry, eventType);
                repository.saveLog(logEntry);
            });
        } catch (Exception e) {
            log.error("Failed to parse SES webhook payload", e);
        }
    }

    private void updateLogEntry(CampaignLog logEntry, String type) {
        switch (type.toUpperCase()) {
            case "DELIVERY" -> {
                if (logEntry.markDelivered()) {
                    repository.incrementCampaignMetric(logEntry.getCampaignId(), METRIC_DELIVERED);
                }
            }
            case "BOUNCE" -> {
                if (logEntry.markBounced()) {
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "BOUNCED");
                    repository.incrementCampaignMetric(logEntry.getCampaignId(), METRIC_BOUNCED);
                }
            }
            case "COMPLAINT" -> {
                if (logEntry.markComplained()) {
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "COMPLAINED");
                }
            }
        }
    }
}