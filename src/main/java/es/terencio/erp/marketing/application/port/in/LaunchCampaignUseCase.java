package es.terencio.erp.marketing.application.port.in;

import java.time.Instant;
import java.util.UUID;

public interface LaunchCampaignUseCase {
    void launchCampaign(UUID companyId, Long campaignId);
    void relaunchCampaign(UUID companyId, Long campaignId);
    void scheduleCampaign(UUID companyId, Long campaignId, Instant scheduledAt);
    void cancelCampaign(UUID companyId, Long campaignId);
    void dryRun(UUID companyId, Long templateId, String testEmail);
}