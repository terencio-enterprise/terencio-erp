package es.terencio.erp.shared.domain.exception;

import es.terencio.erp.shared.exception.DomainException;

/**
 * Thrown when an optimistic-locking version conflict is detected during
 * persistence.
 */
public class OptimisticLockingException extends DomainException {

    public OptimisticLockingException(String message) {
        super(message);
    }
}
