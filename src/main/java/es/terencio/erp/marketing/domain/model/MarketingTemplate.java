package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketingTemplate {
    private Long id;
    private UUID companyId;
    private String code;
    private String name;
    private String subjectTemplate;
    private String bodyHtml;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MarketingAttachment> attachments;

    public String compile(java.util.Map<String, String> variables) {
        String compiledBody = bodyHtml;
        for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
            compiledBody = compiledBody.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return compiledBody;
    }

    public String compileSubject(java.util.Map<String, String> variables) {
        String compiledSubject = subjectTemplate;
        for (java.util.Map.Entry<String, String> entry : variables.entrySet()) {
            compiledSubject = compiledSubject.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return compiledSubject;
    }
}
