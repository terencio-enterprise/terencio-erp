package es.terencio.erp.organization.presentation;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.organization.application.port.in.CreateCompanyUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.application.usecase.CreateCompanyCommand;
import es.terencio.erp.organization.application.usecase.CreateCompanyResult;
import es.terencio.erp.organization.application.usecase.UpdateFiscalSettingsCommand;
import es.terencio.erp.organization.application.usecase.UpdateFiscalSettingsResult;
import es.terencio.erp.organization.application.usecase.UpdateFiscalSettingsService;
import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for Company management (Organization module).
 */
@RestController
@RequestMapping("/api/v1/companies")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Companies", description = "Company profile and fiscal settings management")
public class CompanyController {

    private final CreateCompanyUseCase createCompanyUseCase;
    private final UpdateFiscalSettingsService updateFiscalSettingsService;
    private final CompanyRepository companyRepository;

    public CompanyController(
            CreateCompanyUseCase createCompanyUseCase,
            UpdateFiscalSettingsService updateFiscalSettingsService,
            CompanyRepository companyRepository) {
        this.createCompanyUseCase = createCompanyUseCase;
        this.updateFiscalSettingsService = updateFiscalSettingsService;
        this.companyRepository = companyRepository;
    }

    @PostMapping
    @Operation(summary = "Create company", description = "Creates a new company and initial configuration")
    public ResponseEntity<ApiResponse<CreateCompanyResult>> createCompany(
            @Valid @RequestBody CreateCompanyCommand command) {
        CreateCompanyResult result = createCompanyUseCase.execute(command);
        return ResponseEntity.ok(ApiResponse.success("Company created successfully", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company", description = "Returns company details by identifier")
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(@PathVariable UUID id) {
        Company company = companyRepository.findById(new CompanyId(id))
                .orElseThrow(() -> new RuntimeException("Company not found"));

        return ResponseEntity.ok(ApiResponse.success("Company fetched successfully", new CompanyResponse(
                company.id().value(),
                company.name(),
                company.taxId().value(),
                company.currencyCode(),
                company.fiscalRegime().name(),
                company.priceIncludesTax(),
                company.roundingMode().name(),
                company.isActive())));
    }

    @PutMapping("/{id}/fiscal-settings")
    @Operation(summary = "Update fiscal settings", description = "Updates company fiscal configuration settings")
    public ResponseEntity<ApiResponse<UpdateFiscalSettingsResult>> updateFiscalSettings(
            @Parameter(description = "Company identifier") @PathVariable UUID id,
            @Valid @RequestBody UpdateFiscalSettingsCommand command) {
        UpdateFiscalSettingsResult result = updateFiscalSettingsService.execute(id, command);
        return ResponseEntity.ok(ApiResponse.success("Fiscal settings updated successfully", result));
    }

    public record CompanyResponse(
            UUID id,
            String name,
            String taxId,
            String currencyCode,
            String fiscalRegime,
            boolean priceIncludesTax,
            String roundingMode,
            boolean active) {
    }
}
