package es.terencio.erp.organization.application.service;

import java.util.UUID;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsCommand;
import es.terencio.erp.organization.application.dto.OrganizationCommands.UpdateFiscalSettingsResult;
import es.terencio.erp.organization.application.port.in.UpdateFiscalSettingsUseCase;
import es.terencio.erp.organization.application.port.out.CompanyRepository;
import es.terencio.erp.organization.domain.model.Company;
import es.terencio.erp.organization.domain.model.FiscalRegime;
import es.terencio.erp.organization.domain.model.RoundingMode;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.exception.DomainException;

public class UpdateFiscalSettingsService implements UpdateFiscalSettingsUseCase {
    private final CompanyRepository companyRepository;

    public UpdateFiscalSettingsService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Override
    public UpdateFiscalSettingsResult execute(UUID companyId, UpdateFiscalSettingsCommand command) {
        Company company = companyRepository.findById(new CompanyId(companyId)).orElseThrow(() -> new DomainException("Company not found"));
        
        FiscalRegime fiscalRegime = command.fiscalRegime() != null ? FiscalRegime.valueOf(command.fiscalRegime()) : company.fiscalRegime();
        boolean priceIncludesTax = command.priceIncludesTax() != null ? command.priceIncludesTax() : company.priceIncludesTax();
        RoundingMode roundingMode = command.roundingMode() != null ? RoundingMode.valueOf(command.roundingMode()) : company.roundingMode();

        company.configureFiscalSettings(fiscalRegime, priceIncludesTax, roundingMode);
        companyRepository.save(company);

        return new UpdateFiscalSettingsResult(companyId, fiscalRegime.name(), priceIncludesTax, roundingMode.name());
    }
}
