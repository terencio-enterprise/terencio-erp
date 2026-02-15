package es.terencio.erp.audit.application.port.out;

import es.terencio.erp.audit.domain.model.AuditUserAction;

/**
 * Output port for AuditUserAction persistence.
 */
public interface AuditRepository {

    void log(AuditUserAction action);
}
