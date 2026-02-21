package es.terencio.erp.marketing.application.dto.customer;

import es.terencio.erp.marketing.domain.model.MarketingStatus;

public record MarketingCustomer(
    Long id,
    String email,
    String name,
    boolean canReceiveMarketing,
    MarketingStatus status,
    String unsubscribeToken
) {}