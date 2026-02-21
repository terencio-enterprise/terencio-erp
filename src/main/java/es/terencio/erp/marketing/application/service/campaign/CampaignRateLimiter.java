package es.terencio.erp.marketing.application.service.campaign;

import com.google.common.util.concurrent.RateLimiter;

public class CampaignRateLimiter {
    private final RateLimiter delegate;

    public CampaignRateLimiter(double rateLimitPerSecond) {
        this.delegate = RateLimiter.create(rateLimitPerSecond);
    }

    public void acquire() {
        delegate.acquire();
    }
}