package es.terencio.erp.marketing.application.port.in;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import es.terencio.erp.marketing.application.dto.TemplateDto;

public interface ManageTemplatesUseCase {
    List<TemplateDto> listTemplates(String search);

    TemplateDto getTemplate(Long id);

    TemplateDto createTemplate(TemplateDto template);

    TemplateDto updateTemplate(Long id, TemplateDto template);

    void deleteTemplate(Long id);

    void addAttachment(Long templateId, MultipartFile file);

    void removeAttachment(Long templateId, Long attachmentId);
}
