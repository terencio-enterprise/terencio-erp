package es.terencio.erp.organization.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyResult;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsResult;
import es.terencio.erp.organization.application.port.in.CompanyUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.organization.domain.model.FiscalRegime;
import es.terencio.erp.organization.domain.model.RoundingMode;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.exception.DomainException;

public class CompanyService implements CompanyUseCase {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    @Transactional
    public CreateCompanyResult create(CreateCompanyCommand command) {
        if (companyRepository.existsByTaxId(command.taxId())) {
            throw new DomainException("Company with tax ID already exists: " + command.taxId());
        }

        Company company = Company.create(command.name(), command.taxId(), command.currencyCode() != null ? command.currencyCode() : "EUR");

        if (command.fiscalRegime() != null || command.roundingMode() != null) {
            FiscalRegime regime = command.fiscalRegime() != null ? FiscalRegime.valueOf(command.fiscalRegime()) : FiscalRegime.COMMON;
            RoundingMode rounding = command.roundingMode() != null ? RoundingMode.valueOf(command.roundingMode()) : RoundingMode.LINE;
            company.configureFiscalSettings(regime, command.priceIncludesTax(), rounding);
        }

        Company saved = companyRepository.save(company);
        return new CreateCompanyResult(saved.id().value(), saved.name(), saved.taxId().value());
    }

    @Override
    @Transactional(readOnly = true)
    public Company getById(UUID companyId) {
        return companyRepository.findById(new CompanyId(companyId))
                .orElseThrow(() -> new DomainException("Company not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> getAllForEmployee(UUID employeeUuid) {
        return companyRepository.findVisibleCompaniesByEmployeeUuid(employeeUuid);
    }

    @Override
    @Transactional
    public UpdateFiscalSettingsResult updateFiscalSettings(UUID companyId, UpdateFiscalSettingsCommand command) {
        Company company = getById(companyId);

        FiscalRegime fiscalRegime = command.fiscalRegime() != null ? FiscalRegime.valueOf(command.fiscalRegime()) : company.fiscalRegime();
        boolean priceIncludesTax = command.priceIncludesTax() != null ? command.priceIncludesTax() : company.priceIncludesTax();
        RoundingMode roundingMode = command.roundingMode() != null ? RoundingMode.valueOf(command.roundingMode()) : company.roundingMode();

        company.configureFiscalSettings(fiscalRegime, priceIncludesTax, roundingMode);
        companyRepository.save(company);

        return new UpdateFiscalSettingsResult(companyId, fiscalRegime.name(), priceIncludesTax, roundingMode.name());
    }
}