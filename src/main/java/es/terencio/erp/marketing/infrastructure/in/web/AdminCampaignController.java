package es.terencio.erp.marketing.infrastructure.in.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignRequest;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignResult;
import es.terencio.erp.marketing.application.port.in.LaunchCampaignUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/campaigns")
@Tag(name = "Marketing Campaigns", description = "Campaign launch, history and analytics endpoints")
public class AdminCampaignController {

    private final LaunchCampaignUseCase launchCampaignUseCase;
    private final CampaignRepositoryPort campaignRepository;

    public AdminCampaignController(LaunchCampaignUseCase launchCampaignUseCase, CampaignRepositoryPort campaignRepository) {
        this.launchCampaignUseCase = launchCampaignUseCase;
        this.campaignRepository = campaignRepository;
    }

    @GetMapping
    @Operation(summary = "List campaigns")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<CampaignLog>>> getCampaignHistory(@PathVariable UUID companyId, @Parameter(description = "Campaign status filter") @RequestParam(required = false) String status) {
        List<CampaignLog> logs = campaignRepository.findLogsByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get campaign stats")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_VIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampaignStats(@PathVariable UUID companyId, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(Map.of("sent", 1000, "bounced", 5, "openRate", "20%")));
    }

    @PostMapping("/dry-run")
    @Operation(summary = "Run campaign dry-run")
    @RequiresPermission(permission = Permission.MARKETING_EMAIL_PREVIEW, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> dryRun(@PathVariable UUID companyId, @RequestBody Map<String, Object> payload) {
        Long templateId = ((Number) payload.get("templateId")).longValue();
        String testEmail = (String) payload.get("testEmail");
        launchCampaignUseCase.dryRun(templateId, testEmail);
        return ResponseEntity.ok(ApiResponse.success("Dry run requested"));
    }

    @PostMapping
    @Operation(summary = "Launch campaign")
    @RequiresPermission(permission = Permission.MARKETING_CAMPAIGN_LAUNCH, scope = AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResult>> launchCampaign(@PathVariable UUID companyId, @RequestBody CampaignRequest request) {
        CampaignResult result = launchCampaignUseCase.launch(request.templateId(), request.audienceFilter());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
