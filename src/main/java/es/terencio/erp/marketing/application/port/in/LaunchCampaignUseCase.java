package es.terencio.erp.marketing.application.port.in;

import es.terencio.erp.marketing.application.dto.CampaignRequest;
import es.terencio.erp.marketing.application.dto.CampaignResult;

public interface LaunchCampaignUseCase {
    CampaignResult launch(Long templateId, CampaignRequest.AudienceFilter filter);

    void dryRun(Long templateId, String testEmail);
}
