package es.terencio.erp.marketing.application.port.in;

import java.util.UUID;

public interface CampaignLaunchUseCase {
    void launchCampaign(UUID companyId, Long campaignId);
    void relaunchCampaign(UUID companyId, Long campaignId);
    void dryRun(UUID companyId, Long templateId, String testEmail);
}