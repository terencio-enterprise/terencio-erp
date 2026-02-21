package es.terencio.erp.marketing.infrastructure.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.marketing.application.port.in.ProcessWebhookUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/public/marketing/webhook")
@Tag(name = "Public Webhooks", description = "Provider webhooks (SES, SNS)")
public class PublicWebhookController {

    private final ProcessWebhookUseCase webhookUseCase;

    public PublicWebhookController(ProcessWebhookUseCase webhookUseCase) {
        this.webhookUseCase = webhookUseCase;
    }

    @PostMapping(value = "/ses", consumes = {"text/plain", "application/json"})
    @Operation(summary = "Receive AWS SES/SNS Webhook for Bounces and Complaints")
    public ResponseEntity<Void> handleSesWebhook(@RequestBody String payload) {
        webhookUseCase.processSesEvent(payload);
        return ResponseEntity.ok().build();
    }
}