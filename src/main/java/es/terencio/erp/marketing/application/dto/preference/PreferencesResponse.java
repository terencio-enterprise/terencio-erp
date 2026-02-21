package es.terencio.erp.marketing.application.dto.preference;

import es.terencio.erp.marketing.domain.model.MarketingStatus;

public record PreferencesResponse(
        String email,
        MarketingStatus status
) {}