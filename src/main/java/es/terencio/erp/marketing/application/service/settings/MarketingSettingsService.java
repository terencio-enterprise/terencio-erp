package es.terencio.erp.marketing.application.service.settings;

import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.settings.MarketingSettingsDto;
import es.terencio.erp.marketing.application.port.in.MarketingSettingsUseCase;
import es.terencio.erp.marketing.application.port.out.MarketingSettingsRepositoryPort;
import es.terencio.erp.marketing.domain.model.CompanyMarketingSettings;

public class MarketingSettingsService implements MarketingSettingsUseCase {

    private final MarketingSettingsRepositoryPort repository;

    public MarketingSettingsService(MarketingSettingsRepositoryPort repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public MarketingSettingsDto getSettings(UUID companyId) {
        CompanyMarketingSettings settings = repository.findByCompanyId(companyId)
                .orElseGet(() -> CompanyMarketingSettings.defaultSettings(companyId));
        return toDto(settings);
    }

    @Override
    @Transactional
    public MarketingSettingsDto updateSettings(UUID companyId, MarketingSettingsDto request) {
        CompanyMarketingSettings settings = repository.findByCompanyId(companyId)
                .orElseGet(() -> CompanyMarketingSettings.defaultSettings(companyId));

        settings.update(
                request.senderName(),
                request.senderEmail(),
                request.dailySendLimit(),
                request.welcomeEmailActive(),
                request.welcomeTemplateId(),
                request.welcomeDelayMinutes()
        );

        return toDto(repository.save(settings));
    }

    private MarketingSettingsDto toDto(CompanyMarketingSettings s) {
        return new MarketingSettingsDto(
                s.getSenderName(),
                s.getSenderEmail(),
                s.isDomainVerified(),
                s.getDailySendLimit(),
                s.isWelcomeEmailActive(),
                s.getWelcomeTemplateId(),
                s.getWelcomeDelayMinutes()
        );
    }
}