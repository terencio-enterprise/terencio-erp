package es.terencio.erp.marketing.infrastructure.in.web;

import java.time.Instant;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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
import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.campaign.CampaignResponse;
import es.terencio.erp.marketing.application.dto.campaign.CreateCampaignRequest;
import es.terencio.erp.marketing.application.port.in.CampaignLaunchUseCase;
import es.terencio.erp.marketing.application.port.in.CampaignManagementUseCase;
import es.terencio.erp.marketing.application.port.in.CampaignQueryUseCase;
import es.terencio.erp.shared.domain.query.PageResult;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/campaigns")
@Tag(name = "Marketing Campaigns", description = "Campaign launch, history and analytics endpoints")
@Validated
public class AdminCampaignController {

    private final CampaignManagementUseCase campaignManagementUseCase;
    private final CampaignQueryUseCase campaignQueryUseCase;
    private final CampaignLaunchUseCase campaignLaunchUseCase;

    public AdminCampaignController(CampaignManagementUseCase campaignManagementUseCase,
            CampaignQueryUseCase campaignQueryUseCase, CampaignLaunchUseCase campaignLaunchUseCase) {
        this.campaignManagementUseCase = campaignManagementUseCase;
        this.campaignQueryUseCase = campaignQueryUseCase;
        this.campaignLaunchUseCase = campaignLaunchUseCase;
    }

    @PostMapping("/draft")
    @Operation(summary = "Create Campaign Draft")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> createDraft(@PathVariable UUID companyId, @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(campaignManagementUseCase.createDraft(companyId, request)));
    }

    @PutMapping("/{id}/draft")
    @Operation(summary = "Update Campaign Draft")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateDraft(@PathVariable UUID companyId, @PathVariable Long id, @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(campaignManagementUseCase.updateDraft(companyId, id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Campaign details and metrics")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(campaignQueryUseCase.getCampaign(companyId, id)));
    }

    @GetMapping("/{id}/audience")
    @Operation(summary = "Get affected customers for campaign")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<PageResult<CampaignAudienceMember>>> getAudience(
            @PathVariable UUID companyId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                campaignQueryUseCase.getCampaignAudience(companyId, id, page, size)
        ));
    }

    @PostMapping("/{id}/launch")
    @Operation(summary = "Launch campaign asynchronously immediately")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> launchCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        campaignLaunchUseCase.launchCampaign(companyId, id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Campaign enqueued successfully."));
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule campaign for future launch")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> scheduleCampaign(@PathVariable UUID companyId, @PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledAt) {
        campaignManagementUseCase.scheduleCampaign(companyId, id, scheduledAt);
        return ResponseEntity.ok(ApiResponse.success("Campaign scheduled successfully."));
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(summary = "Cancel a scheduled campaign")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> cancelCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        campaignManagementUseCase.cancelCampaign(companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Campaign cancelled successfully."));
    }

    @PostMapping("/{id}/relaunch")
    @Operation(summary = "Relaunch campaign to un-emailed audience members")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> relaunchCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        campaignLaunchUseCase.relaunchCampaign(companyId, id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Campaign relaunch enqueued."));
    }

    @PostMapping("/dry-run")
    @Operation(summary = "Send test email for template")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> dryRun(@PathVariable UUID companyId, @RequestParam Long templateId, @RequestParam String testEmail) {
        campaignLaunchUseCase.dryRun(companyId, templateId, testEmail);
        return ResponseEntity.ok(ApiResponse.success("Test email dispatched."));
    }
}