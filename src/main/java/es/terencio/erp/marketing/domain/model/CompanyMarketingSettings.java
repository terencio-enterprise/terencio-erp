package es.terencio.erp.marketing.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import lombok.Getter;

@Getter
public class CompanyMarketingSettings {
    private final UUID companyId;
    private String senderName;
    private String senderEmail;
    private boolean domainVerified;
    private int dailySendLimit;
    
    private boolean welcomeEmailActive;
    private Long welcomeTemplateId;
    private int welcomeDelayMinutes;
    
    private Instant updatedAt;

    public CompanyMarketingSettings(UUID companyId, String senderName, String senderEmail, boolean domainVerified,
                                    int dailySendLimit, boolean welcomeEmailActive, Long welcomeTemplateId,
                                    int welcomeDelayMinutes, Instant updatedAt) {
        this.companyId = companyId;
        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.domainVerified = domainVerified;
        this.dailySendLimit = dailySendLimit;
        this.welcomeEmailActive = welcomeEmailActive;
        this.welcomeTemplateId = welcomeTemplateId;
        this.welcomeDelayMinutes = welcomeDelayMinutes;
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public static CompanyMarketingSettings defaultSettings(UUID companyId) {
        return new CompanyMarketingSettings(
                companyId, null, null, false, 500, false, null, 5, Instant.now()
        );
    }

    public void update(String senderName, String senderEmail, int dailySendLimit,
                       boolean welcomeEmailActive, Long welcomeTemplateId, int welcomeDelayMinutes) {
        if (dailySendLimit < 0) {
            throw new InvariantViolationException("Daily send limit cannot be negative");
        }
        if (welcomeEmailActive && welcomeTemplateId == null) {
            throw new InvariantViolationException("A Welcome Template is required if the welcome email is active");
        }

        this.senderName = senderName;
        this.senderEmail = senderEmail;
        this.dailySendLimit = dailySendLimit;
        this.welcomeEmailActive = welcomeEmailActive;
        this.welcomeTemplateId = welcomeTemplateId;
        this.welcomeDelayMinutes = welcomeDelayMinutes;
        this.updatedAt = Instant.now();
    }
}