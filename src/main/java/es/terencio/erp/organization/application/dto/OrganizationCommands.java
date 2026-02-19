package es.terencio.erp.organization.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public final class OrganizationCommands {
    private OrganizationCommands() {}

    public record CreateCompanyCommand(UUID organizationId, String name, String taxId, String currencyCode, String fiscalRegime, boolean priceIncludesTax, String roundingMode) {}
    public record CreateCompanyResult(UUID companyId, String name, String taxId) {}

    public record CreateStoreCommand(UUID companyId, String code, String name, String street, String zipCode, String city, String taxId, String timezone) {}
    public record CreateStoreResult(UUID storeId, UUID warehouseId, String storeCode, String storeName) {}

    public record UpdateFiscalSettingsCommand(String fiscalRegime, Boolean priceIncludesTax, String roundingMode) {}
    public record UpdateFiscalSettingsResult(UUID companyId, String fiscalRegime, boolean priceIncludesTax, String roundingMode) {}

    public record UpdateStoreSettingsCommand(UUID storeId, boolean allowNegativeStock, Long defaultTariffId, boolean printTicketAutomatically, BigDecimal requireCustomerForLargeAmount) {}
}
