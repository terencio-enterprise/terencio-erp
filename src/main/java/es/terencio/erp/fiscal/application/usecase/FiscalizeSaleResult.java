package es.terencio.erp.fiscal.application.usecase;

/**
 * Result of fiscalizing a sale.
 */
public record FiscalizeSaleResult(
        Long fiscalLogId,
        String recordHash,
        int chainSequence) {
}
