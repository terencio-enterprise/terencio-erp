package es.terencio.erp.marketing.infrastructure.in.web;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.marketing.application.dto.settings.MarketingSettingsDto;
import es.terencio.erp.marketing.application.port.in.MarketingSettingsUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/settings")
@Tag(name = "Marketing Settings", description = "Company-wide marketing module configuration")
public class AdminMarketingSettingsController {

    private final MarketingSettingsUseCase marketingSettingsUseCase;

    public AdminMarketingSettingsController(MarketingSettingsUseCase marketingSettingsUseCase) {
        this.marketingSettingsUseCase = marketingSettingsUseCase;
    }

    @GetMapping
    @Operation(summary = "Get marketing settings")
    @RequiresPermission(permission = Permission.MARKETING_SETTINGS_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<MarketingSettingsDto>> getSettings(@PathVariable UUID companyId) {
        return ResponseEntity.ok(ApiResponse.success(marketingSettingsUseCase.getSettings(companyId)));
    }

    @PutMapping
    @Operation(summary = "Update marketing settings")
    @RequiresPermission(permission = Permission.MARKETING_SETTINGS_EDIT, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<MarketingSettingsDto>> updateSettings(
            @PathVariable UUID companyId,
            @Valid @RequestBody MarketingSettingsDto request) {
        return ResponseEntity.ok(ApiResponse.success(marketingSettingsUseCase.updateSettings(companyId, request)));
    }
}