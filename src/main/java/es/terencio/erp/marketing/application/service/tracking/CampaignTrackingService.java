package es.terencio.erp.marketing.application.service.tracking;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.port.in.CampaignTrackingUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.service.campaign.TrackingLinkService;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;

public class CampaignTrackingService implements CampaignTrackingUseCase {
    private static final Logger log = LoggerFactory.getLogger(CampaignTrackingService.class);
    private static final byte[] PIXEL_BYTES = Base64.getDecoder()
            .decode("R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7");
            
    private static final String METRIC_OPENED = "opened";
    private static final String METRIC_CLICKED = "clicked";

    private final CampaignRepositoryPort campaignRepository;
    private final TrackingLinkService trackingLinkService;
    private final MarketingProperties properties;

    public CampaignTrackingService(CampaignRepositoryPort campaignRepository, TrackingLinkService trackingLinkService, MarketingProperties properties) {
        this.campaignRepository = campaignRepository;
        this.trackingLinkService = trackingLinkService;
        this.properties = properties;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public byte[] registerOpenAndGetPixel(Long logId) {
        campaignRepository.findLogById(logId).ifPresent(entry -> {
            if (entry.markOpened()) {
                campaignRepository.saveLog(entry);
                campaignRepository.incrementCampaignMetric(entry.getCampaignId(), METRIC_OPENED);
            }
        });
        return PIXEL_BYTES;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String registerClickAndGetRedirectUrl(Long logId, String encodedPayload, String signature) {
        if (!trackingLinkService.generateHmac(encodedPayload).equals(signature)) {
            log.error("Invalid click signature for log: {}", logId);
            return properties.getPublicBaseUrl();
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(encodedPayload));
            String[] parts = decoded.split("\\|");
            String originalUrl = parts[0];
            long expiresAt = Long.parseLong(parts[1]);
            
            if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
                log.warn("Invalid scheme on redirect: {}", originalUrl);
                return properties.getPublicBaseUrl();
            }

            List<String> allowedDomains = properties.getAllowedRedirectDomains();
            if (allowedDomains != null && !allowedDomains.isEmpty()) {
                URI uri = URI.create(originalUrl);
                if (!allowedDomains.contains(uri.getHost())) {
                    log.warn("Security Block: Attempted redirect to unauthorized host: {}", uri.getHost());
                    return properties.getPublicBaseUrl();
                }
            }

            if (Instant.now().toEpochMilli() > expiresAt) {
                log.warn("Expired tracking link: {}", logId);
                return originalUrl;
            }

            campaignRepository.findLogById(logId).ifPresent(entry -> {
                if (entry.markClicked()) {
                    campaignRepository.saveLog(entry);
                    campaignRepository.incrementCampaignMetric(entry.getCampaignId(), METRIC_CLICKED);
                }
            });
            return originalUrl;
        } catch (Exception e) {
            log.error("Click tracking resolution failed for log {}", logId, e);
            return properties.getPublicBaseUrl();
        }
    }
}