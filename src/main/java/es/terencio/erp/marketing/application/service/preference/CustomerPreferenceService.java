package es.terencio.erp.marketing.application.service.preference;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.preference.PreferencesResponse;
import es.terencio.erp.marketing.application.dto.preference.UnsubscribeRequest;
import es.terencio.erp.marketing.application.port.in.CustomerPreferenceUseCase;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort.MarketingCustomer;
import es.terencio.erp.marketing.domain.model.MarketingStatus;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

public class CustomerPreferenceService implements CustomerPreferenceUseCase {
    private final CustomerIntegrationPort customerPort;

    public CustomerPreferenceService(CustomerIntegrationPort customerPort) {
        this.customerPort = customerPort;
    }

    @Override
    @Transactional(readOnly = true)
    public PreferencesResponse getPreferences(String token) {
        MarketingCustomer customer = customerPort.findByToken(token).orElseThrow(() -> new ResourceNotFoundException("Invalid Token"));
        MarketingStatus mappedStatus = customer.canReceiveMarketing() ? MarketingStatus.SUBSCRIBED : MarketingStatus.UNSUBSCRIBED;
        return new PreferencesResponse(customer.email(), mappedStatus);
    }

    @Override
    @Transactional
    public void updatePreferences(UnsubscribeRequest request) {
        String token = request.token();
        if (MarketingStatus.UNSUBSCRIBED == request.action()) {
            customerPort.updateMarketingStatus(token, MarketingStatus.UNSUBSCRIBED, null);
        } else if (MarketingStatus.SNOOZED == request.action() && request.snoozeDays() != null) {
            Instant snoozeUntil = Instant.now().plus(request.snoozeDays(), ChronoUnit.DAYS);
            customerPort.updateMarketingStatus(token, MarketingStatus.SNOOZED, snoozeUntil);
        }
    }

    @Override
    @Transactional
    public void unsubscribeOneClick(String token) {
        customerPort.updateMarketingStatus(token, MarketingStatus.UNSUBSCRIBED, null);
    }
}