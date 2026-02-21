package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.Getter;

@Getter
public class CampaignLog {

    private Long id;
    private Long campaignId;
    private UUID companyId;
    private Long customerId;
    private Long templateId;

    private DeliveryStatus status;

    private Instant sentAt;
    private Instant deliveredAt;
    private Instant openedAt;
    private Instant clickedAt;
    private Instant bouncedAt;
    private Instant unsubscribedAt;

    private String messageId;
    private String errorMessage;

    public void markSent(String messageId) {
        this.status = DeliveryStatus.SENT;
        this.sentAt = Instant.now();
        this.messageId = messageId;
    }

    public void markDelivered() {
        if (deliveredAt == null) {
            this.deliveredAt = Instant.now();
            this.status = DeliveryStatus.DELIVERED;
        }
    }

    public void markOpened() {
        if (openedAt == null) {
            this.openedAt = Instant.now();
            this.status = DeliveryStatus.OPENED;
        }
    }

    public void markClicked() {
        if (clickedAt == null) {
            this.clickedAt = Instant.now();
        }
    }

    public void markBounced() {
        this.status = DeliveryStatus.BOUNCED;
        this.bouncedAt = Instant.now();
    }

    public void markFailed(String error) {
        this.status = DeliveryStatus.FAILED;
        this.errorMessage = error;
    }
}