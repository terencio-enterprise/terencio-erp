package es.terencio.erp.marketing.application.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

@Data
public class CampaignRequest {
    private String name;
    private Long templateId;
    private AudienceFilter audienceFilter;

    @Data
    public static class AudienceFilter {
        private List<String> tags;
        private BigDecimal minSpent;
        private String customerType; // e.g., LEAD, CLIENT
    }
}
