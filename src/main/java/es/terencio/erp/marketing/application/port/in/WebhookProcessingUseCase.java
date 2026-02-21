package es.terencio.erp.marketing.application.port.in;

public interface WebhookProcessingUseCase {
    void processSesEvent(String payload);
}