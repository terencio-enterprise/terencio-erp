package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;
import es.terencio.erp.marketing.application.dto.TemplateDto;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.StorageSystemPort;
import es.terencio.erp.marketing.domain.model.MarketingAttachment;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService implements ManageTemplatesUseCase {

    private final CampaignRepositoryPort repository;
    private final StorageSystemPort storage;

    @Value("${terencio.marketing.s3.bucket}")
    private String s3Bucket;

    @Override
    public List<TemplateDto> listTemplates(String search) {
        return repository.findAllTemplates(search).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public TemplateDto getTemplate(Long id) {
        return repository.findTemplateById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
    }

    @Override
    public TemplateDto createTemplate(TemplateDto dto) {
        MarketingTemplate template = MarketingTemplate.builder()
                .companyId(getCompanyIdFromContext())
                .code(dto.getCode())
                .name(dto.getName())
                .subjectTemplate(dto.getSubject())
                .bodyHtml(dto.getBodyHtml())
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .attachments(new ArrayList<>())
                .build();

        return toDto(repository.saveTemplate(template));
    }

    @Override
    public TemplateDto updateTemplate(Long id, TemplateDto dto) {
        MarketingTemplate template = repository.findTemplateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));

        template.setName(dto.getName());
        template.setCode(dto.getCode());
        template.setSubjectTemplate(dto.getSubject());
        template.setBodyHtml(dto.getBodyHtml());
        template.setUpdatedAt(Instant.now());

        return toDto(repository.saveTemplate(template));
    }

    @Override
    public void deleteTemplate(Long id) {
        repository.deleteTemplate(id);
    }

    @Override
    public void addAttachment(Long templateId, MultipartFile file) {
        MarketingTemplate template = repository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));

        String key = "templates/" + templateId + "/" + file.getOriginalFilename();

        storage.upload(file, key);

        MarketingAttachment attachment = MarketingAttachment.builder()
                .templateId(templateId)
                .filename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .s3Bucket(s3Bucket)
                .s3Key(key)
                .build();

        template.getAttachments().add(attachment);
        repository.saveTemplate(template);
    }

    @Override
    public void removeAttachment(Long templateId, Long attachmentId) {
        MarketingTemplate template = repository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));

        template.getAttachments().removeIf(a -> {
            if (a.getId().equals(attachmentId)) {
                storage.delete(a.getS3Bucket(), a.getS3Key());
                return true;
            }
            return false;
        });

        repository.saveTemplate(template);
    }

    private TemplateDto toDto(MarketingTemplate t) {
        return TemplateDto.builder()
                .id(t.getId())
                .code(t.getCode())
                .name(t.getName())
                .subject(t.getSubjectTemplate())
                .bodyHtml(t.getBodyHtml())
                .active(t.isActive())
                .lastModified(t.getUpdatedAt())
                .attachments(t.getAttachments().stream().map(a -> TemplateDto.AttachmentDto.builder()
                        .id(a.getId())
                        .filename(a.getFilename())
                        .size(a.getFileSizeBytes())
                        .contentType(a.getContentType())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    private UUID getCompanyIdFromContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getCompanyId();
        }
        // Fallback or throw exception
        throw new RuntimeException("No valid company context found");
    }
}
