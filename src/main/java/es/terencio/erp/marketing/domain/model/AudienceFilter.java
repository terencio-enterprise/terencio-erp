package es.terencio.erp.marketing.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record AudienceFilter(
        List<String> tags, 
        BigDecimal minSpent, 
        String customerType
) {}