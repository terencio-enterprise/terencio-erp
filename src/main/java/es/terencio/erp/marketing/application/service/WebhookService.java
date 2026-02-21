package es.terencio.erp.marketing.application.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.port.in.ProcessWebhookUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailDeliveryEvent;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;

public class WebhookService implements ProcessWebhookUseCase {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final CampaignRepositoryPort repository;
    private final MarketingProperties properties;

    public WebhookService(CampaignRepositoryPort repository, MarketingProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void processSesEvent(String payload) {
        // Idempotency: Basic check if this payload was already received (in reality, check MessageId+Timestamp)
        
        // AWS SES JSON extraction logic
        String messageId = extractField(payload, "\"messageId\"\\s*:\\s*\"([^\"]+)\"");
        String eventType = extractField(payload, "\"notificationType\"\\s*:\\s*\"([^\"]+)\"");
        String email = extractField(payload, "\"destination\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");
        
        if (messageId == null || eventType == null) {
            log.warn("Invalid Webhook Payload received: {}", payload);
            return;
        }

        EmailDeliveryEvent event = new EmailDeliveryEvent(null, messageId, email, eventType, null, null, payload, null);
        repository.saveDeliveryEvent(event);

        repository.findLogByMessageId(messageId).ifPresent(logEntry -> {
            if ("Bounce".equalsIgnoreCase(eventType)) {
                if (logEntry.getStatus() != DeliveryStatus.BOUNCED) {
                    logEntry.setStatus(DeliveryStatus.BOUNCED);
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "BOUNCED");
                    repository.incrementCampaignMetric(logEntry.getCampaignId(), "bounced");
                }
            } else if ("Complaint".equalsIgnoreCase(eventType)) {
                if (logEntry.getStatus() != DeliveryStatus.COMPLAINED) {
                    logEntry.setStatus(DeliveryStatus.COMPLAINED);
                    repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "COMPLAINED");
                }
            }
            repository.saveLog(logEntry);
            log.info("Processed {} for messageId: {}. Customer deactivated.", eventType, messageId);
        });
    }

    private String extractField(String payload, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }
}
