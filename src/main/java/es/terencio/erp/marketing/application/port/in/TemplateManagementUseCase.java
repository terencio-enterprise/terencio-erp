package es.terencio.erp.marketing.application.port.in;

import java.util.List;
import java.util.UUID;

import es.terencio.erp.marketing.application.dto.template.TemplateDto;

public interface TemplateManagementUseCase {
    List<TemplateDto> listTemplates(UUID companyId, String search);
    TemplateDto getTemplate(Long id);
    TemplateDto createTemplate(UUID companyId, TemplateDto template);
    TemplateDto updateTemplate(Long id, TemplateDto template);
    void deleteTemplate(Long id);
}