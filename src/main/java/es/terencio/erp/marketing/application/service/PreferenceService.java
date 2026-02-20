package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import es.terencio.erp.marketing.application.dto.MarketingDtos.UnsubscribeRequest;
import es.terencio.erp.marketing.application.port.in.ManagePreferencesUseCase;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort.MarketingCustomer;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class PreferenceService implements ManagePreferencesUseCase {
    private final CustomerIntegrationPort customerPort;

    public PreferenceService(CustomerIntegrationPort customerPort) {
        this.customerPort = customerPort;
    }

    @Override
    public Map<String, Object> getPreferences(String token) {
        MarketingCustomer customer = customerPort.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Invalid Token"));
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("email", customer.email());
        prefs.put("status", customer.canReceiveMarketing() ? "SUBSCRIBED" : "UNSUBSCRIBED");
        return prefs;
    }

    @Override
    @Transactional
    public void updatePreferences(UnsubscribeRequest request) {
        String token = request.token();
        if ("UNSUBSCRIBE".equals(request.action())) {
            customerPort.updateMarketingStatus(token, "UNSUBSCRIBED", null);
        } else if ("SNOOZE".equals(request.action()) && request.snoozeDays() != null) {
            Instant snoozeUntil = Instant.now().plus(request.snoozeDays(), ChronoUnit.DAYS);
            customerPort.updateMarketingStatus(token, "SNOOZED", snoozeUntil);
        }
    }

    @Override
    @Transactional
    public void unsubscribeOneClick(String token) {
        customerPort.updateMarketingStatus(token, "UNSUBSCRIBED", null);
    }
}