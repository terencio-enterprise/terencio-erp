package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.UnsubscribeRequest;
import es.terencio.erp.marketing.application.port.in.ManagePreferencesUseCase;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreferenceService implements ManagePreferencesUseCase {

    private final CustomerIntegrationPort customerPort;

    @Override
    public Map<String, Object> getPreferences(String token) {
        CustomerIntegrationPort.MarketingCustomer customer = customerPort.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Token"));

        Map<String, Object> prefs = new HashMap<>();
        prefs.put("email", customer.getEmail());
        prefs.put("status", customer.isCanReceiveMarketing() ? "SUBSCRIBED" : "UNSUBSCRIBED");
        return prefs;
    }

    @Override
    @Transactional
    public void updatePreferences(UnsubscribeRequest request) {
        String token = request.getToken();

        if ("UNSUBSCRIBE".equals(request.getAction())) {
            customerPort.updateMarketingStatus(token, "UNSUBSCRIBED", null);
        } else if ("SNOOZE".equals(request.getAction()) && request.getSnoozeDays() != null) {
            Instant snoozeUntil = Instant.now().plus(request.getSnoozeDays(), ChronoUnit.DAYS);
            customerPort.updateMarketingStatus(token, "SNOOZED", snoozeUntil);
        }
    }

    @Override
    @Transactional
    public void unsubscribeOneClick(String token) {
        customerPort.updateMarketingStatus(token, "UNSUBSCRIBED", null);
    }
}
