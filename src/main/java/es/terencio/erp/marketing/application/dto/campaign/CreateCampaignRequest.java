package es.terencio.erp.marketing.application.dto.campaign;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name cannot be blank") String name, 
        @NotNull(message = "Template ID is required") Long templateId, 
        AudienceFilterDto audienceFilter
) {}