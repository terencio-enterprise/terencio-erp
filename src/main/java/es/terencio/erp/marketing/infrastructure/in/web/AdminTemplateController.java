package es.terencio.erp.marketing.infrastructure.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.marketing.application.dto.MarketingDtos.TemplateDto;
import es.terencio.erp.marketing.application.port.in.ManageTemplatesUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/templates")
@Tag(name = "Marketing Templates", description = "Template CRUD operations")
public class AdminTemplateController {

    private final ManageTemplatesUseCase manageTemplatesUseCase;

    public AdminTemplateController(ManageTemplatesUseCase manageTemplatesUseCase) {
        this.manageTemplatesUseCase = manageTemplatesUseCase;
    }

    @GetMapping
    @Operation(summary = "List templates")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<TemplateDto>>> listTemplates(@PathVariable UUID companyId, @RequestParam(required = false) String search) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.listTemplates(companyId, search)));
    }

    @PostMapping
    @Operation(summary = "Create template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_CREATE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> createTemplate(@PathVariable UUID companyId, @RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.createTemplate(companyId, template)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> getTemplate(@PathVariable UUID companyId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.getTemplate(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_EDIT, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<TemplateDto>> updateTemplate(@PathVariable UUID companyId, @PathVariable Long id, @RequestBody TemplateDto template) {
        return ResponseEntity.ok(ApiResponse.success(manageTemplatesUseCase.updateTemplate(id, template)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template")
    @RequiresPermission(permission = Permission.MARKETING_TEMPLATE_DELETE, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable UUID companyId, @PathVariable Long id) {
        manageTemplatesUseCase.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted"));
    }
}