package es.terencio.erp.marketing.application.port.in;

import es.terencio.erp.marketing.application.dto.preference.PreferencesResponse;
import es.terencio.erp.marketing.application.dto.preference.UnsubscribeRequest;

public interface CustomerPreferenceUseCase {
    PreferencesResponse getPreferences(String token);
    void updatePreferences(UnsubscribeRequest request);
    void unsubscribeOneClick(String token);
}