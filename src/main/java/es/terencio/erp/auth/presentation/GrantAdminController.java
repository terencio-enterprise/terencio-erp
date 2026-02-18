package es.terencio.erp.auth.presentation;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.application.dto.GrantMatrixDto;
import es.terencio.erp.auth.application.dto.UpdateGrantRequest;
import es.terencio.erp.auth.application.service.GrantAdministrationService;
import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
@Tag(name = "Admin Grants", description = "Management of granular permissions")
public class GrantAdminController {

    private final GrantAdministrationService grantService;

    public GrantAdminController(GrantAdministrationService grantService) {
        this.grantService = grantService;
    }

    @GetMapping("/employees/{id}/grants")
    @Operation(summary = "List employee grants")
    public ResponseEntity<ApiResponse<GrantListResponse>> getEmployeeGrants(@PathVariable Long id) {
        List<GrantAdministrationService.GrantSummaryDto> grants = grantService.getEmployeeGrants(id);
        return ResponseEntity.ok(ApiResponse.success("Grants fetched", new GrantListResponse(id, grants)));
    }

    @GetMapping("/grants/{grantId}/matrix")
    @Operation(summary = "Get permission matrix")
    public ResponseEntity<ApiResponse<GrantMatrixDto>> getGrantMatrix(@PathVariable Long grantId) {
        return ResponseEntity.ok(ApiResponse.success("Matrix fetched", grantService.getGrantMatrix(grantId)));
    }

    @PutMapping("/grants/{grantId}")
    @Operation(summary = "Update grant permissions")
    public ResponseEntity<ApiResponse<Void>> updateGrant(
            @PathVariable Long grantId,
            @RequestBody UpdateGrantRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        grantService.updateGrant(grantId, request, user);
        return ResponseEntity.ok(ApiResponse.success("Grant updated"));
    }

    public record GrantListResponse(Long employeeId, List<GrantAdministrationService.GrantSummaryDto> grants) {
    }
}
