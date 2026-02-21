package es.terencio.erp.marketing.application.dto.preference;

import es.terencio.erp.marketing.domain.model.MarketingStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UnsubscribeRequest(
        @NotBlank(message = "Token is required") String token,
        @NotNull(message = "Action is required") MarketingStatus action,
        Integer snoozeDays,
        String reason) {
    public UnsubscribeRequest(String token, String actionStr, Integer snoozeDays, String reason) {
        this(token, MarketingStatus.parseOrDefault(actionStr, null), snoozeDays, reason);
    }
}