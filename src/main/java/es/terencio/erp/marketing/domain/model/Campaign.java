package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single execution log of a campaign email sent to a customer.
 * Maps to 'marketing_logs' table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {
    private Long id;
    private UUID companyId;
    private Long customerId;
    private Long templateId;
    private Instant sentAt;
    private DeliveryStatus status;
    private String messageId;
    private String errorMessage;
}
