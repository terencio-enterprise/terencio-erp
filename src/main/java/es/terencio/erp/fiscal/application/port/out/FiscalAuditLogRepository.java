package es.terencio.erp.fiscal.application.port.out;

import java.util.Optional;

import es.terencio.erp.fiscal.domain.model.FiscalAuditLog;
import es.terencio.erp.shared.domain.identifier.DeviceId;

/**
 * Output port for FiscalAuditLog persistence.
 */
public interface FiscalAuditLogRepository {

    FiscalAuditLog save(FiscalAuditLog log);

    Optional<FiscalAuditLog> findLastByDevice(DeviceId deviceId);

    int getNextSequenceForDevice(DeviceId deviceId);
}
