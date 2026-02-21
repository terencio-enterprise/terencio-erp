package es.terencio.erp.marketing.application.port.in;

import java.util.UUID;

import es.terencio.erp.marketing.application.dto.settings.MarketingSettingsDto;

public interface MarketingSettingsUseCase {
    MarketingSettingsDto getSettings(UUID companyId);
    MarketingSettingsDto updateSettings(UUID companyId, MarketingSettingsDto request);
}