package es.terencio.erp.marketing.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class MarketingDtos {
    private MarketingDtos() {}

    public record AudienceFilter(List<String> tags, BigDecimal minSpent, String customerType) {}
    public record CampaignRequest(String name, Long templateId, AudienceFilter audienceFilter) {}
    public record CampaignResult(int sentCount) {}
    
    public record TemplateDto(Long id, String code, String name, String subject, String bodyHtml, boolean active, Instant lastModified) {}
    
    public record UnsubscribeRequest(String token, String action, Integer snoozeDays, String reason) {}
}
