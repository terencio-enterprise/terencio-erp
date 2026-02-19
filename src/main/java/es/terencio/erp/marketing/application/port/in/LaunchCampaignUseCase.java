package es.terencio.erp.marketing.application.port.in;

import es.terencio.erp.marketing.application.dto.MarketingDtos.AudienceFilter;
import es.terencio.erp.marketing.application.dto.MarketingDtos.CampaignResult;

public interface LaunchCampaignUseCase {
    CampaignResult launch(Long templateId, AudienceFilter filter);
    void dryRun(Long templateId, String testEmail);
}
