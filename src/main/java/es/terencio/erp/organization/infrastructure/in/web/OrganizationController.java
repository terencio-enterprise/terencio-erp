package es.terencio.erp.organization.infrastructure.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.infrastructure.config.security.CustomUserDetails;
import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;
import es.terencio.erp.organization.application.port.in.OrganizationTreeUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationTreeUseCase organizationTreeUseCase;

    public OrganizationController(OrganizationTreeUseCase organizationTreeUseCase) {
        this.organizationTreeUseCase = organizationTreeUseCase;
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<CompanyTreeDto>>> getOrganizationTree(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<CompanyTreeDto> companies = organizationTreeUseCase.getCompanyTreeForEmployee(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Organization tree retrieved successfully", companies));
    }

    @PutMapping("/context")
    public ResponseEntity<ApiResponse<Void>> switchContext(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody SwitchContextRequest request) {
        organizationTreeUseCase.switchContext(userDetails.getId(), request.companyId(), request.storeId());
        return ResponseEntity.ok(ApiResponse.success("Context switched successfully"));
    }

    public record SwitchContextRequest(UUID companyId, UUID storeId) {}
}
