package es.terencio.erp.marketing.application.port.out;

import java.util.Optional;
import java.util.UUID;

import es.terencio.erp.marketing.domain.model.CompanyMarketingSettings;

public interface MarketingSettingsRepositoryPort {
    Optional<CompanyMarketingSettings> findByCompanyId(UUID companyId);
    CompanyMarketingSettings save(CompanyMarketingSettings settings);
}