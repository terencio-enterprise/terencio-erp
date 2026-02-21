package es.terencio.erp.marketing.application.port.in;

import java.util.Map;

import es.terencio.erp.marketing.application.dto.MarketingDtos.UnsubscribeRequest;

public interface ManagePreferencesUseCase {
    Map<String, Object> getPreferences(String token);
    void updatePreferences(UnsubscribeRequest request);
    void unsubscribeOneClick(String token);
}