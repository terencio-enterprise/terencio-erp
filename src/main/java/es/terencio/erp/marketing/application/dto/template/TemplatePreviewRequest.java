package es.terencio.erp.marketing.application.dto.template;

import java.util.Map;

public record TemplatePreviewRequest(
        Map<String, String> variables
) {}