package es.terencio.erp.marketing.domain.model;

public enum MarketingStatus {
    SUBSCRIBED,
    UNSUBSCRIBED,
    SNOOZED,
    BLOCKED;

    public static MarketingStatus parseOrDefault(String rawValue, MarketingStatus defaultValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            return MarketingStatus.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return defaultValue;
        }
    }
}