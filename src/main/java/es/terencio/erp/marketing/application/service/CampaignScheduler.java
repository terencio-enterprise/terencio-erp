package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import es.terencio.erp.marketing.application.port.in.LaunchCampaignUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.domain.model.MarketingCampaign;

@Component
public class CampaignScheduler {
    private static final Logger log = LoggerFactory.getLogger(CampaignScheduler.class);
    private static final String LOCK_NAME = "marketing_campaign_scheduler";
    
    private final CampaignRepositoryPort repository;
    private final LaunchCampaignUseCase launchCampaignUseCase;

    public CampaignScheduler(CampaignRepositoryPort repository, LaunchCampaignUseCase launchCampaignUseCase) {
        this.repository = repository;
        this.launchCampaignUseCase = launchCampaignUseCase;
    }   

    @Scheduled(cron = "0 * * * * *")
    public void launchScheduledCampaigns() {
        if (!repository.acquireSchedulerLock(LOCK_NAME)) {
            log.debug("Scheduler lock acquired by another instance. Skipping.");
            return;
        }

        try {
            List<MarketingCampaign> dueCampaigns = repository.findScheduledCampaignsToLaunch(Instant.now());
            for (MarketingCampaign campaign : dueCampaigns) {
                log.info("Scheduler: Auto-launching Scheduled Campaign ID: {} for Company: {}", campaign.getId(), campaign.getCompanyId());
                launchCampaignUseCase.launchCampaign(campaign.getCompanyId(), campaign.getId());
            }
        } finally {
            repository.releaseSchedulerLock(LOCK_NAME);
        }
    }
}