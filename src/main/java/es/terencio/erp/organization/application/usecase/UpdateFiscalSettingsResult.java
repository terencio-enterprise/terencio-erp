package es.terencio.erp.organization.application.usecase;

import java.util.UUID;

/**
 * Result of updating fiscal settings.
 */
public record UpdateFiscalSettingsResult(
        UUID companyId,
        String fiscalRegime,
        boolean priceIncludesTax,
        String roundingMode) {
}
