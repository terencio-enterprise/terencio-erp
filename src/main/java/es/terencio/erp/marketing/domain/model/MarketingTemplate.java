package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarketingTemplate {

    private Long id;
    private final UUID companyId;
    private String code;
    private String name;
    private String subjectTemplate;
    private String bodyHtml;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;
    private List<MarketingAttachment> attachments;

    public MarketingTemplate(
            Long id,
            UUID companyId,
            String code,
            String name,
            String subjectTemplate,
            String bodyHtml,
            boolean active,
            Instant createdAt,
            Instant updatedAt,
            List<MarketingAttachment> attachments) {
        if (companyId == null)
            throw new IllegalArgumentException("MarketingTemplate companyId is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("MarketingTemplate name is required");
        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.subjectTemplate = subjectTemplate;
        this.bodyHtml = bodyHtml;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
    }

    // ==================== Domain Methods ====================

    public String compile(Map<String, String> variables) {
        String compiledBody = bodyHtml;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            compiledBody = compiledBody.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return compiledBody;
    }

    public String compileSubject(Map<String, String> variables) {
        String compiledSubject = subjectTemplate;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            compiledSubject = compiledSubject.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return compiledSubject;
    }

    /**
     * Domain method for updating template content.
     * Enforces that name is non-blank.
     */
    public void update(String name, String code, String subjectTemplate, String bodyHtml) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Template name cannot be blank");
        this.name = name;
        this.code = code;
        this.subjectTemplate = subjectTemplate;
        this.bodyHtml = bodyHtml;
        this.updatedAt = Instant.now();
    }

    // ==================== Getters ====================

    public Long getId() {
        return id;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<MarketingAttachment> getAttachments() {
        return attachments;
    }

    // ==================== Infrastructure-only setters ====================

    /** Called after DB insert to set the generated ID. */
    public void setId(Long id) {
        this.id = id;
    }

    /** Called by infrastructure adapter when loading attachments from DB. */
    public void setAttachments(List<MarketingAttachment> attachments) {
        this.attachments = attachments != null ? new ArrayList<>(attachments) : new ArrayList<>();
    }
}
