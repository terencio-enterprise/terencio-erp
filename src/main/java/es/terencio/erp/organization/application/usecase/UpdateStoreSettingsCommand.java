package es.terencio.erp.organization.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Command to update store settings.
 */
public record UpdateStoreSettingsCommand(
        UUID storeId,
        boolean allowNegativeStock,
        Long defaultTariffId,
        boolean printTicketAutomatically,
        BigDecimal requireCustomerForLargeAmount) {
}
