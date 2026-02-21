package es.terencio.erp.marketing.application.dto.campaign;

import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.MarketingStatus;

public record CampaignAudienceMember(
        Long customerId,
        String email,
        String name,
        MarketingStatus marketingStatus,
        DeliveryStatus sendStatus,
        String unsubscribeToken
) {
        public CampaignAudienceMember(long customerId, String email, String name, String marketingStatusStr, String unsubscribeToken) {
            this(customerId, email, name, 
                 marketingStatusStr != null ? MarketingStatus.valueOf(marketingStatusStr) : null, 
                 null, 
                 unsubscribeToken);
        }
}