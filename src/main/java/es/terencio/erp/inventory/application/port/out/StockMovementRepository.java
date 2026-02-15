package es.terencio.erp.inventory.application.port.out;

import es.terencio.erp.inventory.domain.model.StockMovement;

/**
 * Output port for StockMovement persistence.
 */
public interface StockMovementRepository {

    StockMovement save(StockMovement movement);
}
