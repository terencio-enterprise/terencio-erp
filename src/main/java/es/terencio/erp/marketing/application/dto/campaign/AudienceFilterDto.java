package es.terencio.erp.marketing.application.dto.campaign;

import java.math.BigDecimal;
import java.util.List;

public record AudienceFilterDto(
        List<String> tags, 
        BigDecimal minSpent, 
        String customerType
) {}