package es.terencio.erp.organization.application.dto;

import java.util.List;
import java.util.UUID;

public final class OrganizationDtos {
    private OrganizationDtos() {}

    // Read Models & Responses
    public record CompanyDto(UUID id, String name, String taxId, String currencyCode, String fiscalRegime, boolean priceIncludesTax, String roundingMode, boolean active) {}
    public record StoreDto(UUID id, String code, String name, String address, boolean active) {}
    public record StoreSettingsDto(boolean allowNegativeStock, Long defaultTariffId, boolean printTicketAutomatically, long requireCustomerForLargeAmountCents) {}

    // Tree Representations
    public record StoreTreeDto(UUID id, String name, String slug, String code, UUID companyId) {}
    public record CompanyTreeDto(UUID id, String name, String slug, UUID organizationId, List<StoreTreeDto> stores) {}
    public record OrganizationTreeDto(UUID id, String name, String slug, List<CompanyTreeDto> companies) {}
    public record DashboardContextDto(CompanyTreeDto activeCompany, StoreTreeDto activeStore, List<CompanyTreeDto> availableCompanies) {}
}