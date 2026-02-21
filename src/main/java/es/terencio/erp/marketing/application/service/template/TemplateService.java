package es.terencio.erp.marketing.application.service.template;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.template.TemplateDto;
import es.terencio.erp.marketing.application.dto.template.TemplatePreviewResponse;
import es.terencio.erp.marketing.application.port.in.TemplateManagementUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.TemplateEnginePort;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class TemplateService implements TemplateManagementUseCase {
    
    private final CampaignRepositoryPort repository;
    private final TemplateEnginePort templateEngine;

    public TemplateService(CampaignRepositoryPort repository, TemplateEnginePort templateEngine) {
        this.repository = repository;
        this.templateEngine = templateEngine;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> listTemplates(UUID companyId, String search) {
        return repository.findAllTemplates(companyId, search).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateDto getTemplate(Long id) {
        return repository.findTemplateById(id).map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
    }

    @Override
    @Transactional
    public TemplateDto createTemplate(UUID companyId, TemplateDto dto) {
        MarketingTemplate template = new MarketingTemplate(null, companyId, dto.code(), dto.name(), dto.subject(),
                dto.bodyHtml(), true, Instant.now(), Instant.now());
        MarketingTemplate t = repository.saveTemplate(template);
        return toDto(t);
    }

    @Override
    @Transactional
    public TemplateDto updateTemplate(Long id, TemplateDto dto) {
        MarketingTemplate template = repository.findTemplateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        template.update(dto.name(), dto.code(), dto.subject(), dto.bodyHtml());
        return toDto(repository.saveTemplate(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        repository.deleteTemplate(id);
    }

    @Override
    @Transactional(readOnly = true)
    public TemplatePreviewResponse previewTemplate(UUID companyId, Long id, Map<String, String> variables) {
        MarketingTemplate template = repository.findTemplateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        
        if (!template.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException("Template not found");
        }

        // Apply fallback dummy variables for a clean preview
        Map<String, String> vars = new java.util.HashMap<>(variables != null ? variables : Map.of());
        vars.putIfAbsent("name", "John Doe");
        vars.putIfAbsent("unsubscribe_link", "https://example.com/api/v1/public/marketing/preferences?token=demo");

        String compiledSubject = template.compileSubject(vars, templateEngine);
        String compiledBody = template.compile(vars, templateEngine);

        return new TemplatePreviewResponse(compiledSubject, compiledBody);
    }

    private TemplateDto toDto(MarketingTemplate t) {
        return new TemplateDto(t.getId(), t.getCode(), t.getName(), t.getSubjectTemplate(), t.getBodyHtml(),
                t.isActive(), t.getUpdatedAt());
    }
}