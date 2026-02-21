package es.terencio.erp.marketing.application.port.in;

public interface CampaignTrackingUseCase {
    byte[] registerOpenAndGetPixel(Long logId);
    String registerClickAndGetRedirectUrl(Long logId, String originalUrl);
}
