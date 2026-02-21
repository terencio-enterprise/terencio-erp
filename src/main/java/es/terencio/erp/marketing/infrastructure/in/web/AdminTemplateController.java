package es.terencio.erp.marketing.infrastructure.in.web;

import java.util.List;
import java.util.UUID;

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

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.marketing.application.dto.template.TemplateDto;
import es.terencio.erp.marketing.application.port.in.TemplateManagementUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/templates")
@Tag(name = "Marketing Templates", description = "Template CRUD operations")
public class AdminTemplateController {

    private final TemplateManagementUseCase templateManagementUseCase;

    public AdminTemplateController(TemplateManagementUseCase templateManagementUseCase) {
        this.templateManagementUseCase = templateManagementUseCase;
    }

    @GetMapping
    @Operation(summary = "List templates")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<TemplateDto>>> listTemplates(@PathVariable UUID companyId, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(templateManagementUseCase.listTemplates(companyId, search)));
    }

    @PostMapping
    @Operation(summary = "Create template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> createTemplate(@PathVariable UUID companyId, @RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(templateManagementUseCase.createTemplate(companyId, template)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> getTemplate(@PathVariable UUID companyId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(templateManagementUseCase.getTemplate(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_EDIT, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> updateTemplate(@PathVariable UUID companyId, @PathVariable Long id, @RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(templateManagementUseCase.updateTemplate(id, template)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_DELETE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID companyId, @PathVariable Long id) {
        templateManagementUseCase.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted"));
    }
}