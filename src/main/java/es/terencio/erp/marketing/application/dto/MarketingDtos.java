package es.terencio.erp.marketing.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import es.terencio.erp.marketing.domain.model.CampaignStatus;

public final class MarketingDtos {
    private MarketingDtos() {}

    public record AudienceFilter(List<String> tags, BigDecimal minSpent, String customerType) {}
    
    public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name cannot be blank") String name, 
        @NotNull(message = "Template ID is required") Long templateId, 
        AudienceFilter audienceFilter
    ) {}
    
    public record CampaignResponse(Long id, String name, CampaignStatus status, Instant scheduledAt, 
                                   int metricsTotalRecipients, int metricsSent, int metricsOpened, 
                                   int metricsClicked, int metricsBounced) {}
                                   
    public record CampaignAudienceMember(Long customerId, String email, String name, String status) {}

    public record TemplateDto(Long id, String code, String name, String subject, String bodyHtml, boolean active, Instant lastModified) {}
    
    public record UnsubscribeRequest(
        @NotBlank(message = "Token is required") String token, 
        @NotBlank(message = "Action is required") String action, 
        Integer snoozeDays, 
        String reason
    ) {}
}
