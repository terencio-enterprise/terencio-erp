package es.terencio.erp.organization.infrastructure.in.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.CustomUserDetails;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyResult;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsResult;
import es.terencio.erp.organization.application.port.in.CompanyUseCase;
import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Companies", description = "Company profile and fiscal settings management")
public class CompanyController {

    private final CompanyUseCase companyUseCase;

    public CompanyController(CompanyUseCase companyUseCase) {
        this.companyUseCase = companyUseCase;
    }

    @PostMapping
    @Operation(summary = "Create company")
    @RequiresPermission(permission = Permission.ORGANIZATION_COMPANY_CREATE, scope = AccessScope.ORGANIZATION, targetIdParam = "organizationId")
    public ResponseEntity<ApiResponse<CreateCompanyResult>> createCompany(@Valid @RequestBody CreateCompanyCommand command) {
        return ResponseEntity.ok(ApiResponse.success("Company created successfully", companyUseCase.create(command)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company")
    @RequiresPermission(permission = Permission.ORGANIZATION_COMPANY_VIEW, scope = AccessScope.COMPANY, targetIdParam = "id")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(@PathVariable UUID id) {
        Company company = companyUseCase.getById(id);
        return ResponseEntity.ok(ApiResponse.success(
            "Company fetched successfully", 
            new CompanyResponse(company.id().value(), company.name(), company.taxId().value(), company.currencyCode(), company.fiscalRegime().name(), company.priceIncludesTax(), company.roundingMode().name(), company.isActive())
        ));
    }

    @GetMapping
    @Operation(summary = "Get user companies")
    public ResponseEntity<ApiResponse<List<CompanyResponse>>> getCompanies(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<Company> companies = companyUseCase.getAllForEmployee(userDetails.getUuid());
        return ResponseEntity.ok(ApiResponse.success(
            "Companies fetched successfully", 
            companies.stream().map(company -> new CompanyResponse(company.id().value(), company.name(), company.taxId().value(), company.currencyCode(), company.fiscalRegime().name(), company.priceIncludesTax(), company.roundingMode().name(), company.isActive())).toList()
        ));
    }

    @PutMapping("/{id}/fiscal-settings")
    @Operation(summary = "Update fiscal settings")
    @RequiresPermission(permission = Permission.ORGANIZATION_COMPANY_UPDATE, scope = AccessScope.COMPANY, targetIdParam = "id")
    public ResponseEntity<ApiResponse<UpdateFiscalSettingsResult>> updateFiscalSettings(
            @Parameter(description = "Company identifier") @PathVariable UUID id, 
            @Valid @RequestBody UpdateFiscalSettingsCommand command) {
        return ResponseEntity.ok(ApiResponse.success("Fiscal settings updated successfully", companyUseCase.updateFiscalSettings(id, command)));
    }

    public record CompanyResponse(UUID id, String name, String taxId, String currencyCode, String fiscalRegime, boolean priceIncludesTax, String roundingMode, boolean active) {}
}