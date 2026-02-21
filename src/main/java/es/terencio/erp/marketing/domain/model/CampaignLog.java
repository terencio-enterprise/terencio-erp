package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignLog {
    private Long id;
    private Long campaignId;
    private UUID companyId;
    private Long customerId;
    private Long templateId;
    private Instant sentAt;
    private DeliveryStatus status;
    private String messageId;
    private String errorMessage;
    private Instant openedAt;
    private Instant clickedAt;

    public CampaignLog(Long id, Long campaignId, UUID companyId, Long customerId, Long templateId, 
                       Instant sentAt, DeliveryStatus status, String messageId, String errorMessage) {
        this.id = id;
        this.campaignId = campaignId;
        this.companyId = companyId;
        this.customerId = customerId;
        this.templateId = templateId;
        this.sentAt = sentAt;
        this.status = status;
        this.messageId = messageId;
        this.errorMessage = errorMessage;
    }
}
