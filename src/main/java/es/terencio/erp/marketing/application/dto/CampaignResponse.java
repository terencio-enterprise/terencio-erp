package es.terencio.erp.marketing.application.dto;

import java.time.Instant;

import es.terencio.erp.marketing.domain.model.CampaignStatus;

public record CampaignResponse(
        Long id, 
        String name, 
        CampaignStatus status, 
        Instant scheduledAt, 
        int metricsTotalRecipients, 
        int metricsSent, 
        int metricsDelivered, 
        int metricsOpened, 
        int metricsClicked, 
        int metricsBounced, 
        int metricsUnsubscribed
) {}