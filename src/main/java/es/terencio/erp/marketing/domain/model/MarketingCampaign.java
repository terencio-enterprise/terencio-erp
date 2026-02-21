package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketingCampaign {
    private Long id;
    private UUID companyId;
    private String name;
    private Long templateId;
    private String status; // DRAFT, SCHEDULED, SENDING, COMPLETED
    private Instant scheduledAt;
    
    private int metricsTotalRecipients = 0;
    private int metricsSent = 0;
    private int metricsOpened = 0;
    private int metricsClicked = 0;

    public MarketingCampaign(Long id, UUID companyId, String name, Long templateId, String status) {
        this.id = id;
        this.companyId = companyId;
        this.name = name;
        this.templateId = templateId;
        this.status = status;
    }
}
