package es.terencio.erp.marketing.application.dto.template;

import java.time.Instant;

public record TemplateDto(
        Long id, 
        String code, 
        String name, 
        String subject, 
        String bodyHtml, 
        boolean active, 
        Instant lastModified
) {}