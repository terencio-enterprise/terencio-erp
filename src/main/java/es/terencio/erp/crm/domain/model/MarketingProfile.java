package es.terencio.erp.crm.domain.model;

import java.time.Instant;
import java.util.List;

import es.terencio.erp.shared.domain.utils.SecurityUtils;

public record MarketingProfile(
        String origin,
        List<String> tags,
        boolean consent,
        MarketingStatus status,
        String unsubscribeToken,
        Instant lastInteractionAt
) {

    public static MarketingProfile createLead(String origin, List<String> tags, boolean consent) {
        return new MarketingProfile(
                origin,
                tags != null ? List.copyOf(tags) : List.of(),
                consent,
                consent ? MarketingStatus.SUBSCRIBED : MarketingStatus.UNSUBSCRIBED,
                SecurityUtils.generateSecureToken(),
                Instant.now()
        );
    }

    public static MarketingProfile empty() {
        return new MarketingProfile(
                null,
                List.of(),
                false,
                MarketingStatus.UNSUBSCRIBED,
                SecurityUtils.generateSecureToken(),
                null
        );
    }

    public MarketingProfile unsubscribe() {
        return new MarketingProfile(
                origin,
                tags,
                false,
                MarketingStatus.UNSUBSCRIBED,
                unsubscribeToken,
                Instant.now()
        );
    }

    public MarketingProfile registerInteraction() {
        return new MarketingProfile(
                origin,
                tags,
                consent,
                status,
                unsubscribeToken,
                Instant.now()
        );
    }
}