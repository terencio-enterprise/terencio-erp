package es.terencio.erp.fiscal.application.port.in;

import es.terencio.erp.fiscal.application.usecase.FiscalizeSaleCommand;
import es.terencio.erp.fiscal.application.usecase.FiscalizeSaleResult;

/**
 * Use case for fiscalizing a sale (creating fiscal audit log).
 */
public interface FiscalizeSaleUseCase {
    FiscalizeSaleResult execute(FiscalizeSaleCommand command);
}
