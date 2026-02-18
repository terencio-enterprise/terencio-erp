package es.terencio.erp.marketing.infrastructure.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.infrastructure.security.RequiresPermission;
import es.terencio.erp.marketing.application.dto.CampaignRequest;
import es.terencio.erp.marketing.application.dto.CampaignResult;
import es.terencio.erp.marketing.application.port.in.LaunchCampaignUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.Campaign;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/companies/{companyId}/marketing/campaigns")
@RequiredArgsConstructor

@Tag(name = "Marketing Campaigns", description = "Campaign launch, history and analytics endpoints")
public class AdminCampaignController {

    private final LaunchCampaignUseCase launchCampaignUseCase;
    private final CampaignRepositoryPort campaignRepository;

    @GetMapping
    @Operation(summary = "List campaigns", description = "Returns campaign history, optionally filtered by status")
    @RequiresPermission(permission = "marketing:campaign:view", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<List<Campaign>>> getCampaignHistory(
            @PathVariable UUID companyId,
            @Parameter(description = "Campaign status filter") @RequestParam(required = false) String status) {
        List<Campaign> callbacks = campaignRepository.findLogsByStatus(status); // Simple implementation
        return ResponseEntity.ok(ApiResponse.success(callbacks));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get campaign stats", description = "Returns campaign analytics statistics")
    @RequiresPermission(permission = "marketing:campaign:view", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCampaignStats(
            @PathVariable UUID companyId,
            @PathVariable Long id) {
        // TODO: Implement aggregate stats. For now returning empty or mock.
        return ResponseEntity.ok(ApiResponse.success(Map.of("sent", 1000, "bounced", 5, "openRate", "20%")));
    }

    @PostMapping("/dry-run")
    @Operation(summary = "Run campaign dry-run", description = "Sends test execution for campaign before launch")
    @RequiresPermission(permission = "marketing:email:preview", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<Void>> dryRun(
            @PathVariable UUID companyId,
            @RequestBody Map<String, Object> payload) {
        Long templateId = ((Number) payload.get("templateId")).longValue();
        String testEmail = (String) payload.get("testEmail");
        launchCampaignUseCase.dryRun(templateId, testEmail);
        return ResponseEntity.ok(ApiResponse.success("Dry run requested"));
    }

    @PostMapping
    @Operation(summary = "Launch campaign", description = "Launches a campaign using template and audience filters")
    @RequiresPermission(permission = "marketing:campaign:launch", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResult>> launchCampaign(
            @PathVariable UUID companyId,
            @RequestBody CampaignRequest request) {
        CampaignResult result = launchCampaignUseCase.launch(request.getTemplateId(), request.getAudienceFilter());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/launch")
    @Operation(summary = "Launch campaign (explicit)", description = "Launches a campaign (explicit endpoint)")
    @RequiresPermission(permission = "marketing:campaign:launch", scope = es.terencio.erp.auth.domain.model.AccessScope.COMPANY, targetIdParam = "companyId")
    public ResponseEntity<ApiResponse<CampaignResult>> launchCampaignExplicit(
            @PathVariable UUID companyId,
            @RequestBody CampaignRequest request) {
        return launchCampaign(companyId, request);
    }
}
