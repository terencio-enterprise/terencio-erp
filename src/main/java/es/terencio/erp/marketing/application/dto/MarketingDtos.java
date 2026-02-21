package es.terencio.erp.marketing.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import es.terencio.erp.marketing.domain.model.CampaignStatus;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.MarketingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class MarketingDtos {
    private MarketingDtos() {}

    public record AudienceFilter(List<String> tags, BigDecimal minSpent, String customerType) {}

    public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name cannot be blank") String name, 
        @NotNull(message = "Template ID is required") Long templateId, 
        AudienceFilter audienceFilter
    ) {}

    public record CampaignResponse(Long id, String name, CampaignStatus status, Instant scheduledAt, 
                                   int metricsTotalRecipients, int metricsSent, int metricsDelivered, 
                                   int metricsOpened, int metricsClicked, int metricsBounced, int metricsUnsubscribed) {}
                                   
    public record CampaignAudienceMember(
        Long customerId,
        String email,
        String name,
        MarketingStatus marketingStatus,
        DeliveryStatus sendStatus,
        String unsubscribeToken
    ) {
        public CampaignAudienceMember(long customerId, String email, String name, String marketingStatusStr, String unsubscribeToken) {
            this(customerId, email, name, 
                 marketingStatusStr != null ? MarketingStatus.valueOf(marketingStatusStr) : null, 
                 null, 
                 unsubscribeToken);
        }
    }

    public record TemplateDto(Long id, String code, String name, String subject, String bodyHtml, boolean active, Instant lastModified) {}

    public record UnsubscribeRequest(
        @NotBlank(message = "Token is required") String token, 
        @NotNull(message = "Action is required") MarketingStatus action,
        Integer snoozeDays, 
        String reason
    ) {
        // Overloaded constructor for tests or mapping resolving action string
        public UnsubscribeRequest(String token, String actionStr, Integer snoozeDays, String reason) {
            this(token, actionStr != null ? MarketingStatus.valueOf(actionStr) : null, snoozeDays, reason);
        }
    }
}