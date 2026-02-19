package es.terencio.erp.marketing.domain.model;

import java.time.Instant;

public class EmailDeliveryEvent {
    private Long id;
    private final String providerMessageId;
    private final String emailAddress;
    private final String eventType;
    private final String bounceType;
    private final String bounceSubtype;
    private final String rawPayload;
    private final Instant createdAt;

    public EmailDeliveryEvent(Long id, String providerMessageId, String emailAddress, String eventType,
            String bounceType, String bounceSubtype, String rawPayload, Instant createdAt) {
        this.id = id;
        this.providerMessageId = providerMessageId;
        this.emailAddress = emailAddress;
        this.eventType = eventType;
        this.bounceType = bounceType;
        this.bounceSubtype = bounceSubtype;
        this.rawPayload = rawPayload;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public Long getId() { return id; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getEmailAddress() { return emailAddress; }
    public String getEventType() { return eventType; }
    public String getBounceType() { return bounceType; }
    public String getBounceSubtype() { return bounceSubtype; }
    public String getRawPayload() { return rawPayload; }
    public Instant getCreatedAt() { return createdAt; }
}
