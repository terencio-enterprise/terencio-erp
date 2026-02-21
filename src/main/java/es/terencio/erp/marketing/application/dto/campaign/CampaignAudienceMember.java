package es.terencio.erp.marketing.application.dto.campaign;

import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.MarketingStatus;

public record CampaignAudienceMember(
        Long customerId,
        String email,
        String name,
        MarketingStatus marketingStatus,
        DeliveryStatus sendStatus,
        String unsubscribeToken) {
    public CampaignAudienceMember(long customerId, String email, String name, String marketingStatusStr,
            String unsubscribeToken) {
        this(customerId, email, name,
                MarketingStatus.parseOrDefault(marketingStatusStr, MarketingStatus.UNSUBSCRIBED),
                null,
                unsubscribeToken);
    }

    public CampaignAudienceMember(long customerId, String email, String name, String marketingStatusStr,
            String sendStatusStr, String unsubscribeToken) {
        this(customerId, email, name,
                MarketingStatus.parseOrDefault(marketingStatusStr, MarketingStatus.UNSUBSCRIBED),
                parseDeliveryStatus(sendStatusStr),
                unsubscribeToken);
    }

    private static DeliveryStatus parseDeliveryStatus(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return DeliveryStatus.NOT_SENT;
        }

        try {
            return DeliveryStatus.valueOf(rawValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return DeliveryStatus.NOT_SENT;
        }
    }
}