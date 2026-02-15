package es.terencio.erp.sales.application.port.in;

import es.terencio.erp.sales.application.usecase.CreateSaleDraftCommand;
import es.terencio.erp.sales.application.usecase.CreateSaleDraftResult;

/**
 * Use case for creating a sale draft.
 */
public interface CreateSaleDraftUseCase {
    CreateSaleDraftResult execute(CreateSaleDraftCommand command);
}
