package es.terencio.erp.marketing.application.dto;

import java.time.Instant;
import java.util.List;

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
    private List<AttachmentDto> attachments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttachmentDto {
        private Long id;
        private String filename;
        private Long size;
        private String contentType;
    }
}
