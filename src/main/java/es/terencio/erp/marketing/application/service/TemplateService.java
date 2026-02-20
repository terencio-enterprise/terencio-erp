package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import es.terencio.erp.marketing.application.dto.MarketingDtos.TemplateDto;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class TemplateService implements ManageTemplatesUseCase {
    private final CampaignRepositoryPort repository;

    public TemplateService(CampaignRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> listTemplates(UUID companyId, String search) {
        return repository.findAllTemplates(companyId, search).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateDto getTemplate(Long id) {
        return repository.findTemplateById(id).map(this::toDto).orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
    }

    @Override
    @Transactional
    public TemplateDto createTemplate(UUID companyId, TemplateDto dto) {
        Instant now = Instant.now();
        MarketingTemplate template = new MarketingTemplate(null, companyId, dto.code(), dto.name(), dto.subject(), dto.bodyHtml(), true, now, now);
        return toDto(repository.saveTemplate(template));
    }

    @Override
    @Transactional
    public TemplateDto updateTemplate(Long id, TemplateDto dto) {
        MarketingTemplate template = repository.findTemplateById(id).orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
        template.update(dto.name(), dto.code(), dto.subject(), dto.bodyHtml());
        return toDto(repository.saveTemplate(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        repository.deleteTemplate(id);
    }

    private TemplateDto toDto(MarketingTemplate t) {
        return new TemplateDto(t.getId(), t.getCode(), t.getName(), t.getSubjectTemplate(), t.getBodyHtml(), t.isActive(), t.getUpdatedAt());
    }
}