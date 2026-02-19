package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single execution log of a campaign email sent to a customer.
 * Maps to 'marketing_logs' table.
 */
public class CampaignLog {

    private Long id;
    private final UUID companyId;
    private final Long customerId;
    private final Long templateId;
    private final Instant sentAt;
    private DeliveryStatus status;
    private final String messageId;
    private final String errorMessage;

    public CampaignLog(Long id, UUID companyId, Long customerId, Long templateId, Instant sentAt, DeliveryStatus status,
            String messageId, String errorMessage) {
        if (companyId == null)
            throw new IllegalArgumentException("Campaign companyId is required");
        if (customerId == null)
            throw new IllegalArgumentException("Campaign customerId is required");
        if (templateId == null)
            throw new IllegalArgumentException("Campaign templateId is required");
        if (sentAt == null)
            throw new IllegalArgumentException("Campaign sentAt is required");
        if (status == null)
            throw new IllegalArgumentException("Campaign status is required");
        this.id = id;
        this.companyId = companyId;
        this.customerId = customerId;
        this.templateId = templateId;
        this.sentAt = sentAt;
        this.status = status;
        this.messageId = messageId;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public void setStatus(DeliveryStatus status) {
        this.status = status;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}