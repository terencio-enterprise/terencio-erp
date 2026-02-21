package es.terencio.erp.marketing.application.port.in;

public interface ProcessWebhookUseCase {
    void processSesEvent(String payload);
}