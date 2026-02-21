package es.terencio.erp.marketing.application.dto;

import es.terencio.erp.marketing.application.dto.MarketingDtos.AudienceFilter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name cannot be blank") String name, 
        @NotNull(message = "Template ID is required") Long templateId, 
        AudienceFilter audienceFilter
) {}