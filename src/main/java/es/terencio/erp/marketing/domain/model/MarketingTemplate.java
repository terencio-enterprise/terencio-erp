package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import es.terencio.erp.marketing.application.port.out.TemplateEnginePort;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;

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

    public MarketingTemplate(Long id, UUID companyId, String code, String name, String subjectTemplate, String bodyHtml,
            boolean active, Instant createdAt, Instant updatedAt) {
        if (companyId == null) throw new InvariantViolationException("MarketingTemplate companyId is required");
        if (name == null || name.isBlank()) throw new InvariantViolationException("MarketingTemplate name is required");
        if (subjectTemplate == null || subjectTemplate.isBlank()) throw new InvariantViolationException("MarketingTemplate subject is required");
        if (bodyHtml == null || bodyHtml.isBlank()) throw new InvariantViolationException("MarketingTemplate body is required");
        
        this.id = id;
        this.companyId = companyId;
        this.code = code;
        this.name = name;
        this.subjectTemplate = subjectTemplate;
        this.bodyHtml = bodyHtml;
        this.active = active;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public String compile(Map<String, String> variables, TemplateEnginePort engine) {
        return engine.render(this.bodyHtml, variables);
    }

    public String compileSubject(Map<String, String> variables, TemplateEnginePort engine) {
        return engine.render(this.subjectTemplate, variables);
    }

    public void update(String name, String code, String subjectTemplate, String bodyHtml) {
        if (name == null || name.isBlank()) throw new InvariantViolationException("Template name cannot be blank");
        if (subjectTemplate == null || subjectTemplate.isBlank()) throw new InvariantViolationException("MarketingTemplate subject is required");
        if (bodyHtml == null || bodyHtml.isBlank()) throw new InvariantViolationException("MarketingTemplate body is required");
        
        this.name = name;
        this.code = code;
        this.subjectTemplate = subjectTemplate;
        this.bodyHtml = bodyHtml;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public UUID getCompanyId() { return companyId; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getSubjectTemplate() { return subjectTemplate; }
    public String getBodyHtml() { return bodyHtml; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}