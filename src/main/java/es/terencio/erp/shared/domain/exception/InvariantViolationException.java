package es.terencio.erp.shared.domain.exception;

import es.terencio.erp.shared.exception.DomainException;

/**
 * Thrown when a domain invariant is violated.
 */
public class InvariantViolationException extends DomainException {

    public InvariantViolationException(String message) {
        super(message);
    }
}
