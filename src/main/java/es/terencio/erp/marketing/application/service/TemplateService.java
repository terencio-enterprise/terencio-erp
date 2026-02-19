package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.terencio.erp.marketing.application.dto.TemplateDto;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.StorageSystemPort;
import es.terencio.erp.marketing.domain.model.MarketingAttachment;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

@Service
public class TemplateService implements ManageTemplatesUseCase {

    private final CampaignRepositoryPort repository;
    private final StorageSystemPort storage;

    @Value("${terencio.marketing.s3.bucket}")
    private String s3Bucket;

    public TemplateService(CampaignRepositoryPort repository, StorageSystemPort storage) {
        this.repository = repository;
        this.storage = storage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TemplateDto> listTemplates(UUID companyId, String search) {
        return repository.findAllTemplates(companyId, search).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TemplateDto getTemplate(Long id) {
        return repository.findTemplateById(id)
                .map(this::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));
    }

    @Override
    @Transactional
    public TemplateDto createTemplate(UUID companyId, TemplateDto dto) {
        Instant now = Instant.now();
        MarketingTemplate template = new MarketingTemplate(
                null,
                companyId,
                dto.getCode(),
                dto.getName(),
                dto.getSubject(),
                dto.getBodyHtml(),
                true,
                now,
                now,
                new ArrayList<>());

        return toDto(repository.saveTemplate(template));
    }

    @Override
    @Transactional
    public TemplateDto updateTemplate(Long id, TemplateDto dto) {
        MarketingTemplate template = repository.findTemplateById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + id));

        // Use proper domain method instead of individual setters
        template.update(dto.getName(), dto.getCode(), dto.getSubject(), dto.getBodyHtml());

        return toDto(repository.saveTemplate(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        repository.deleteTemplate(id);
    }

    /**
     * Uploads the file to S3 first, then saves attachment metadata to DB.
     * S3 upload happens OUTSIDE the @Transactional boundary on this method
     * to prevent holding a DB connection while waiting for a slow network call.
     * The method itself is not @Transactional — only the DB save inside is.
     */
    @Override
    public void addAttachment(Long templateId, MultipartFile file) {
        MarketingTemplate template = repository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));

        String key = "templates/" + templateId + "/" + file.getOriginalFilename();

        // S3 upload happens BEFORE the transaction begins
        storage.upload(file, key);

        saveAttachmentToDb(template, file, key);
    }

    @Transactional
    protected void saveAttachmentToDb(MarketingTemplate template, MultipartFile file, String key) {
        MarketingAttachment attachment = new MarketingAttachment(
                null,
                template.getId(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                s3Bucket,
                key,
                null);

        template.getAttachments().add(attachment);
        repository.saveTemplate(template);
    }

    @Override
    public void removeAttachment(Long templateId, Long attachmentId) {
        MarketingTemplate template = repository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));

        // Perform S3 deletion before starting DB transaction
        MarketingAttachment toRemove = template.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found: " + attachmentId));

        storage.delete(toRemove.getS3Bucket(), toRemove.getS3Key());

        removeAttachmentFromDb(template, attachmentId);
    }

    @Transactional
    protected void removeAttachmentFromDb(MarketingTemplate template, Long attachmentId) {
        template.getAttachments().removeIf(a -> a.getId().equals(attachmentId));
        repository.saveTemplate(template);
    }

    private TemplateDto toDto(MarketingTemplate t) {
        List<TemplateDto.AttachmentDto> attachmentDtos = t.getAttachments().stream()
                .map(a -> new TemplateDto.AttachmentDto(a.getId(), a.getFilename(), a.getFileSizeBytes(),
                        a.getContentType()))
                .collect(Collectors.toList());

        return new TemplateDto(
                t.getId(),
                t.getCode(),
                t.getName(),
                t.getSubjectTemplate(),
                t.getBodyHtml(),
                t.isActive(),
                t.getUpdatedAt(),
                attachmentDtos);
    }
}
