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
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignAudienceMember;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignResponse;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CreateCampaignRequest;
import es.terencio.erp.marketing.application.port.in.ManageCampaignsUseCase;
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

    private final ManageCampaignsUseCase manageCampaignsUseCase;

    public AdminCampaignController(ManageCampaignsUseCase manageCampaignsUseCase) {
        this.manageCampaignsUseCase = manageCampaignsUseCase;
    }

    @PostMapping("/draft")
    @Operation(summary = "Create Campaign Draft")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> createDraft(@PathVariable UUID companyId, @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(manageCampaignsUseCase.createDraft(companyId, request)));
    }

    @PutMapping("/{id}/draft")
    @Operation(summary = "Update Campaign Draft")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateDraft(@PathVariable UUID companyId, @PathVariable Long id, @Valid @RequestBody CreateCampaignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(manageCampaignsUseCase.updateDraft(companyId, id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Campaign details and metrics")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(manageCampaignsUseCase.getCampaign(companyId, id)));
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
                manageCampaignsUseCase.getCampaignAudience(companyId, id, page, size)
        ));
    }

    @PostMapping("/{id}/launch")
    @Operation(summary = "Launch campaign asynchronously immediately")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> launchCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        manageCampaignsUseCase.launchCampaign(companyId, id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Campaign enqueued successfully."));
    }

    @PostMapping("/{id}/schedule")
    @Operation(summary = "Schedule campaign for future launch")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> scheduleCampaign(@PathVariable UUID companyId, @PathVariable Long id, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant scheduledAt) {
        manageCampaignsUseCase.scheduleCampaign(companyId, id, scheduledAt);
        return ResponseEntity.ok(ApiResponse.success("Campaign scheduled successfully."));
    }

    @DeleteMapping("/{id}/cancel")
    @Operation(summary = "Cancel a scheduled campaign")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> cancelCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        manageCampaignsUseCase.cancelCampaign(companyId, id);
        return ResponseEntity.ok(ApiResponse.success("Campaign cancelled successfully."));
    }

    @PostMapping("/{id}/relaunch")
    @Operation(summary = "Relaunch campaign to un-emailed audience members")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> relaunchCampaign(@PathVariable UUID companyId, @PathVariable Long id) {
        manageCampaignsUseCase.relaunchCampaign(companyId, id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success("Campaign relaunch enqueued."));
    }

    @PostMapping("/dry-run")
    @Operation(summary = "Send test email for template")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> dryRun(@PathVariable UUID companyId, @RequestParam Long templateId, @RequestParam String testEmail) {
        manageCampaignsUseCase.dryRun(companyId, templateId, testEmail);
        return ResponseEntity.ok(ApiResponse.success("Test email dispatched."));
    }
}