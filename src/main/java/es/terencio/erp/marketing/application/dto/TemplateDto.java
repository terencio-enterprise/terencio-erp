package es.terencio.erp.marketing.application.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateDto {
    private Long id;
    private String code;
    private String name;
    private String subject;
    private String bodyHtml;
    private boolean active;
    private Instant lastModified;
}