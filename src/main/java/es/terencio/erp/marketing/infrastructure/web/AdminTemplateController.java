package es.terencio.erp.marketing.infrastructure.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.terencio.erp.marketing.application.dto.TemplateDto;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/marketing/templates")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
public class AdminTemplateController {

    private final ManageTemplatesUseCase manageTemplatesUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateDto>>> listTemplates(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.listTemplates(search)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateDto>> createTemplate(@RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.createTemplate(template)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDto>> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.getTemplate(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateDto>> updateTemplate(@PathVariable Long id,
            @RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.updateTemplate(id, template)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        manageTemplatesUseCase.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted"));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> uploadAttachment(@PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        manageTemplatesUseCase.addAttachment(id, file);
        return ResponseEntity.ok(ApiResponse.success("Attachment uploaded"));
    }

    @DeleteMapping("/{id}/attachments/{attId}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(@PathVariable Long id, @PathVariable Long attId) {
        manageTemplatesUseCase.removeAttachment(id, attId);
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted"));
    }
}
