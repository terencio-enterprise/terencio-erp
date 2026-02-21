package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
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
    private Instant complainedAt;

    private String messageId;
    private String errorMessage;

    protected CampaignLog() {
        // Required by persistence frameworks
    }

    public static CampaignLog createPending(Long campaignId, UUID companyId, Long customerId, Long templateId) {
        if (campaignId == null || companyId == null || customerId == null || templateId == null) {
            throw new InvariantViolationException("CampaignLog requires campaign, company, customer, and template IDs");
        }
        
        CampaignLog log = new CampaignLog();
        log.campaignId = campaignId;
        log.companyId = companyId;
        log.customerId = customerId;
        log.templateId = templateId;
        log.status = DeliveryStatus.PENDING;
        return log;
    }

    public void markSent(String messageId) {
        if (this.status != DeliveryStatus.PENDING && this.status != DeliveryStatus.FAILED) {
            throw new InvariantViolationException("Only pending or failed logs can be marked as sent");
        }
        if (messageId == null || messageId.isBlank()) {
            throw new InvariantViolationException("Message ID is required when marking as sent");
        }
        this.status = DeliveryStatus.SENT;
        this.sentAt = Instant.now();
        this.messageId = messageId;
    }

    public boolean markDelivered() {
        if (isTerminal()) return false;
        if (this.status == DeliveryStatus.DELIVERED || this.status == DeliveryStatus.OPENED || this.status == DeliveryStatus.CLICKED) return false;
        this.status = DeliveryStatus.DELIVERED;
        this.deliveredAt = Instant.now();
        return true;
    }

    public boolean markOpened() {
        if (isTerminal()) return false;
        if (this.status == DeliveryStatus.PENDING || this.status == DeliveryStatus.FAILED) return false;
        if (this.status == DeliveryStatus.OPENED || this.status == DeliveryStatus.CLICKED) return false;
        this.status = DeliveryStatus.OPENED;
        this.openedAt = Instant.now();
        return true;
    }

    public boolean markClicked() {
        if (isTerminal()) return false;
        if (this.status == DeliveryStatus.PENDING || this.status == DeliveryStatus.FAILED) return false;
        if (this.clickedAt != null) return false;
        this.status = DeliveryStatus.CLICKED;
        this.clickedAt = Instant.now();
        return true;
    }

    public boolean markBounced() {
        if (isTerminal()) return false;
        this.status = DeliveryStatus.BOUNCED;
        this.bouncedAt = Instant.now();
        return true;
    }

    public boolean markComplained() {
        if (isTerminal()) return false;
        this.status = DeliveryStatus.COMPLAINED;
        this.complainedAt = Instant.now();
        return true;
    }

    public void markFailed(String error) {
        this.status = DeliveryStatus.FAILED;
        this.errorMessage = error;
    }

    private boolean isTerminal() {
        return this.status == DeliveryStatus.BOUNCED || this.status == DeliveryStatus.COMPLAINED || this.status == DeliveryStatus.FAILED;
    }
}