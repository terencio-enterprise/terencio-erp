package es.terencio.erp.crm.domain.model.valueobject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import es.terencio.erp.crm.domain.model.MarketingStatus;
import es.terencio.erp.shared.domain.utils.SecurityUtils;
import lombok.Builder;
import lombok.Value;
import lombok.With;

@Value
@Builder
public class MarketingProfile {
    String origin;
    @Builder.Default
    List<String> tags = new ArrayList<>();
    boolean consent;
    @With
    MarketingStatus status;
    String unsubscribeToken;
    Instant lastInteractionAt;
    Instant snoozedUntil;

    public static MarketingProfile createDefault(String origin, List<String> tags) {
        return MarketingProfile.builder()
                .origin(origin)
                .tags(tags != null ? tags : new ArrayList<>())
                .consent(true)
                .status(MarketingStatus.SUBSCRIBED)
                .unsubscribeToken(SecurityUtils.generateSecureToken())
                .build();
    }

    public static MarketingProfile empty() {
        return MarketingProfile.builder().status(MarketingStatus.UNSUBSCRIBED).build();
    }

    public boolean isEligible() {
        if (!consent || status == MarketingStatus.UNSUBSCRIBED || status == MarketingStatus.BOUNCED)
            return false;
        return snoozedUntil == null || !snoozedUntil.isAfter(Instant.now());
    }
}