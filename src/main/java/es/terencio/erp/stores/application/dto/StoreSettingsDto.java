package es.terencio.erp.stores.application.dto;

import java.util.UUID;

public record StoreSettingsDto(
        UUID storeId,
        boolean allowNegativeStock,
        Long defaultTariffId,
        boolean printTicketAutomatically,
        Long requireCustomerForLargeAmount) {
}
