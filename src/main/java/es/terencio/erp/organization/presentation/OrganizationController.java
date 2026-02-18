package es.terencio.erp.organization.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;
import es.terencio.erp.organization.application.dto.OrganizationTreeDto;
import es.terencio.erp.organization.application.service.OrganizationTreeService;
import es.terencio.erp.shared.presentation.ApiResponse;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationTreeService organizationTreeService;

    public OrganizationController(OrganizationTreeService organizationTreeService) {
        this.organizationTreeService = organizationTreeService;
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<OrganizationTreeDto>>> getOrganizationTree(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long employeeId = userDetails.getId();
        List<OrganizationTreeDto> tree = organizationTreeService.getOrganizationTreeForEmployee(employeeId);

        return ResponseEntity.ok(ApiResponse.success("Organization tree retrieved successfully", tree));
    }

    @PutMapping("/context")
    public ResponseEntity<ApiResponse<Void>> switchContext(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SwitchContextRequest request) {

        Long employeeId = userDetails.getId();
        organizationTreeService.switchContext(employeeId, request.companyId(), request.storeId());

        return ResponseEntity.ok(ApiResponse.success("Context switched successfully"));
    }

    public record SwitchContextRequest(UUID companyId, UUID storeId) {
    }
}