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

public class WebhookService implements ProcessWebhookUseCase {
    private static final Logger log = LoggerFactory.getLogger(WebhookService.class);
    private final CampaignRepositoryPort repository;

    public WebhookService(CampaignRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void processSesEvent(String payload) {
        // En producción: usar Jackson para procesar JSON anidado de SNS y SES
        // Para este ERP extraemos datos con Regex seguros por simplicidad
        String messageId = extractField(payload, "\"messageId\"\\s*:\\s*\"([^\"]+)\"");
        String eventType = extractField(payload, "\"notificationType\"\\s*:\\s*\"([^\"]+)\"");
        String email = extractField(payload, "\"destination\"\\s*:\\s*\\[\\s*\"([^\"]+)\"");
        
        if (messageId == null || eventType == null) {
            log.warn("Invalid Webhook Payload received: {}", payload);
            return;
        }

        // Guardamos evidencia cruda
        EmailDeliveryEvent event = new EmailDeliveryEvent(null, messageId, email, eventType, null, null, payload, null);
        repository.saveDeliveryEvent(event);

        // Procesar impacto en reputación
        repository.findLogByMessageId(messageId).ifPresent(logEntry -> {
            if ("Bounce".equalsIgnoreCase(eventType)) {
                logEntry.setStatus(DeliveryStatus.BOUNCED);
                repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "BOUNCED");
                repository.incrementCampaignMetric(logEntry.getCampaignId(), "bounced");
            } else if ("Complaint".equalsIgnoreCase(eventType)) {
                logEntry.setStatus(DeliveryStatus.COMPLAINED);
                repository.markCustomerAsBouncedOrComplained(logEntry.getCustomerId(), "COMPLAINED");
            }
            repository.saveLog(logEntry);
            log.info("Processed {} for messageId: {}. Customer: {}", eventType, messageId, logEntry.getCustomerId());
        });
    }

    private String extractField(String payload, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(payload);
        return matcher.find() ? matcher.group(1) : null;
    }
}
