package es.terencio.erp.shared.exception;

/**
 * Exception thrown when POS registration fails.
 */
public class RegistrationException extends DomainException {

    public RegistrationException(String message) {
        super(message);
    }

    public RegistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
