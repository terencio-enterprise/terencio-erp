package es.terencio.erp.shared.domain.exception;

import es.terencio.erp.shared.exception.DomainException;

/**
 * Thrown when an aggregate is in an invalid state for the requested operation.
 */
public class InvalidStateException extends DomainException {

    public InvalidStateException(String message) {
        super(message);
    }
}
