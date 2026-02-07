package es.terencio.erp.shared.exception;

/**
 * Base exception for domain-level errors.
 * Used to signal business rule violations.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
