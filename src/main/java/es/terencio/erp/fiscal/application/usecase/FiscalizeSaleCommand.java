package es.terencio.erp.fiscal.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Command for fiscalizing a sale.
 */
public record FiscalizeSaleCommand(
        UUID saleUuid,
        UUID storeId,
        UUID deviceId,
        BigDecimal invoiceAmount,
        Instant invoiceDate,
        String softwareId,
        String softwareVersion,
        String developerId) {
}
