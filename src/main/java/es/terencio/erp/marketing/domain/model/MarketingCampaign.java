package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import lombok.Getter;

@Getter
public class MarketingCampaign {

    private Long id;
    private final UUID companyId;
    private final String name;
    private final Long templateId;

    private CampaignStatus status;
    private Instant scheduledAt;
    private Instant startedAt;
    private Instant completedAt;

    private int totalRecipients;
    private int sent;
    private int delivered;
    private int opened;
    private int clicked;
    private int bounced;
    private int unsubscribed;

    private final Instant createdAt;
    private Instant updatedAt;

    // ==========================
    // FACTORY
    // ==========================

    public static MarketingCampaign createDraft(
            UUID companyId,
            String name,
            Long templateId
    ) {
        if (companyId == null)
            throw new InvariantViolationException("Campaign must belong to a company");

        if (name == null || name.isBlank())
            throw new InvariantViolationException("Campaign name cannot be blank");

        if (templateId == null)
            throw new InvariantViolationException("Campaign must have a template");

        return new MarketingCampaign(
                null,
                companyId,
                name,
                templateId,
                CampaignStatus.DRAFT,
                null,
                null,
                null,
                0,0,0,0,0,0,0,
                Instant.now(),
                Instant.now()
        );
    }

    private MarketingCampaign(
            Long id,
            UUID companyId,
            String name,
            Long templateId,
            CampaignStatus status,
            Instant scheduledAt,
            Instant startedAt,
            Instant completedAt,
            int totalRecipients,
            int sent,
            int delivered,
            int opened,
            int clicked,
            int bounced,
            int unsubscribed,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.templateId = templateId;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.totalRecipients = totalRecipients;
        this.sent = sent;
        this.delivered = delivered;
        this.opened = opened;
        this.clicked = clicked;
        this.bounced = bounced;
        this.unsubscribed = unsubscribed;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ==========================
    // STATE TRANSITIONS
    // ==========================

    public void schedule(Instant when) {
        if (status != CampaignStatus.DRAFT)
            throw new InvariantViolationException("Only DRAFT campaigns can be scheduled");

        if (when == null || when.isBefore(Instant.now()))
            throw new InvariantViolationException("Scheduled time must be in the future");

        this.status = CampaignStatus.SCHEDULED;
        this.scheduledAt = when;
        touch();
    }

    public void startSending() {
        if (status != CampaignStatus.DRAFT && status != CampaignStatus.SCHEDULED)
            throw new InvariantViolationException("Campaign cannot start from state: " + status);

        this.status = CampaignStatus.SENDING;
        this.startedAt = Instant.now();
        touch();
    }

    public void complete() {
        if (status != CampaignStatus.SENDING)
            throw new InvariantViolationException("Only SENDING campaigns can be completed");

        this.status = CampaignStatus.COMPLETED;
        this.completedAt = Instant.now();
        touch();
    }

    public void cancel() {
        if (status == CampaignStatus.COMPLETED)
            throw new InvariantViolationException("Completed campaign cannot be cancelled");

        this.status = CampaignStatus.CANCELLED;
        touch();
    }

    // ==========================
    // METRICS
    // ==========================

    public void incrementSent() {
        this.sent++;
    }

    public void incrementDelivered() {
        this.delivered++;
    }

    public void incrementOpened() {
        this.opened++;
    }

    public void incrementClicked() {
        this.clicked++;
    }

    public void incrementBounced() {
        this.bounced++;
    }

    public void incrementUnsubscribed() {
        this.unsubscribed++;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}