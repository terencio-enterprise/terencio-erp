package es.terencio.erp.marketing.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import es.terencio.erp.auth.infrastructure.security.RequiresPermission;
import es.terencio.erp.marketing.application.dto.TemplateDto;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/templates")
@RequiredArgsConstructor

@Tag(name = "Marketing Templates", description = "Template CRUD and attachment management endpoints")
public class AdminTemplateController {

    private final ManageTemplatesUseCase manageTemplatesUseCase;

    @GetMapping
    @Operation(summary = "List templates", description = "Returns templates, optionally filtered by search")
    @RequiresPermission(permission = "marketing:template:view", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<TemplateDto>>> listTemplates(
            @PathVariable UUID companyId,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.listTemplates(companyId, search)));
    }

    @PostMapping
    @Operation(summary = "Create template", description = "Creates a new marketing template")
    @RequiresPermission(permission = "marketing:template:create", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> createTemplate(
            @PathVariable UUID companyId,
            @RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.createTemplate(companyId, template)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template", description = "Returns one template by identifier")
    @RequiresPermission(permission = "marketing:template:view", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> getTemplate(
            @PathVariable UUID companyId,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.getTemplate(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Updates an existing template")
    @RequiresPermission(permission = "marketing:template:edit", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> updateTemplate(
            @PathVariable UUID companyId,
            @PathVariable Long id,
            @RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.updateTemplate(id, template)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", description = "Deletes a template by identifier")
    @RequiresPermission(permission = "marketing:template:delete", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable UUID companyId,
            @PathVariable Long id) {
        manageTemplatesUseCase.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted"));
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload attachment", description = "Uploads a template attachment file")
    @RequiresPermission(permission = "marketing:template:edit", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> uploadAttachment(
            @PathVariable UUID companyId,
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        manageTemplatesUseCase.addAttachment(id, file);
        return ResponseEntity.ok(ApiResponse.success("Attachment uploaded"));
    }

    @DeleteMapping("/{id}/attachments/{attId}")
    @Operation(summary = "Delete attachment", description = "Deletes a template attachment")
    @RequiresPermission(permission = "marketing:template:edit", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable UUID companyId,
            @PathVariable Long id,
            @PathVariable Long attId) {
        manageTemplatesUseCase.removeAttachment(id, attId);
        return ResponseEntity.ok(ApiResponse.success("Attachment deleted"));
    }
}
