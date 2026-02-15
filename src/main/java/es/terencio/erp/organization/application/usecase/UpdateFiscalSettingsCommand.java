package es.terencio.erp.organization.application.usecase;

/**
 * Command for updating company fiscal settings.
 */
public record UpdateFiscalSettingsCommand(
        String fiscalRegime,
        Boolean priceIncludesTax,
        String roundingMode) {
}
