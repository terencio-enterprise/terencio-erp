package es.terencio.erp.marketing.infrastructure.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.marketing.application.dto.MarketingDtos.*;
import es.terencio.erp.marketing.application.port.in.ManageCampaignsUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/campaigns")
@Tag(name = "Marketing Campaigns", description = "Campaign launch, history and analytics endpoints")
public class AdminCampaignController {

    private final ManageCampaignsUseCase manageCampaignsUseCase;

    public AdminCampaignController(ManageCampaignsUseCase manageCampaignsUseCase) {
        this.manageCampaignsUseCase = manageCampaignsUseCase;
    }

    @PostMapping("/draft")
    @Operation(summary = "Create Campaign Draft")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> createDraft(@PathVariable UUID companyId, @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(manageCampaignsUseCase.createDraft(companyId, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Campaign details and metrics")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manageCampaignsUseCase.getCampaign(id)));
    }

    @GetMapping("/{id}/audience")
    @Operation(summary = "Get affected customers for campaign")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<CampaignAudienceMember>>> getAudience(@PathVariable UUID companyId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manageCampaignsUseCase.getCampaignAudience(id)));
    }

    @PostMapping("/{id}/launch")
    @Operation(summary = "Launch campaign asynchronously")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> launchCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        manageCampaignsUseCase.launchCampaign(id);
        return ResponseEntity.ok(ApiResponse.success("Campaign launched successfully in the background."));
    }

    @PostMapping("/{id}/relaunch")
    @Operation(summary = "Relaunch campaign to un-emailed audience members")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> relaunchCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        manageCampaignsUseCase.relaunchCampaign(id);
        return ResponseEntity.ok(ApiResponse.success("Campaign relaunched to remaining audience members."));
    }

    @PostMapping("/dry-run")
    @Operation(summary = "Run campaign dry-run")
    @RequiresPermission(permission = Permission.MARKETING_EMAIL_PREVIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> dryRun(@PathVariable UUID companyId, @RequestBody java.util.Map<String, Object> payload) {
        Long templateId = ((Number) payload.get("templateId")).longValue();
        String testEmail = (String) payload.get("testEmail");
        manageCampaignsUseCase.dryRun(templateId, testEmail);
        return ResponseEntity.ok(ApiResponse.success("Dry run requested"));
    }
}
