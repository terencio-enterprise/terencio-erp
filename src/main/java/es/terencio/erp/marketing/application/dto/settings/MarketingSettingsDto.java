package es.terencio.erp.marketing.application.dto.settings;

public record MarketingSettingsDto(
        String senderName,
        String senderEmail,
        boolean domainVerified,
        int dailySendLimit,
        boolean welcomeEmailActive,
        Long welcomeTemplateId,
        int welcomeDelayMinutes
) {}