package es.terencio.erp.marketing.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.template.TemplateDto;
import es.terencio.erp.marketing.application.dto.template.TemplatePreviewResponse;

public interface TemplateManagementUseCase {
    List<TemplateDto> listTemplates(UUID companyId, String search);
    TemplateDto getTemplate(Long id);
    TemplateDto createTemplate(UUID companyId, TemplateDto template);
    TemplateDto updateTemplate(Long id, TemplateDto template);
    void deleteTemplate(Long id);
    TemplatePreviewResponse previewTemplate(UUID companyId, Long id, Map<String, String> variables);
}