package es.terencio.erp.organization.application.usecase;

/**
 * Command to create a new company.
 */
public record CreateCompanyCommand(
        String name,
        String taxId,
        String currencyCode,
        String fiscalRegime,
        boolean priceIncludesTax,
        String roundingMode) {
}
