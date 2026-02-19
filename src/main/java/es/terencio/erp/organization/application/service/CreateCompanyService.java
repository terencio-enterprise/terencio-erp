package es.terencio.erp.organization.application.service;

import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.CreateCompanyResult;
import es.terencio.erp.organization.application.port.in.CreateCompanyUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.organization.domain.model.FiscalRegime;
import es.terencio.erp.organization.domain.model.RoundingMode;
import es.terencio.erp.shared.exception.DomainException;

public class CreateCompanyService implements CreateCompanyUseCase {
    private final CompanyRepository companyRepository;

    public CreateCompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public CreateCompanyResult execute(CreateCompanyCommand command) {
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
}
