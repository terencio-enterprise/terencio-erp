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
    private Instant deliveredAt;
    private DeliveryStatus status;
    private String messageId;
    private String errorMessage;
    private Instant openedAt;
    private Instant clickedAt;
}
