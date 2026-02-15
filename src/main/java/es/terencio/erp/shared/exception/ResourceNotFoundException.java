package es.terencio.erp.shared.exception;

/**
 * Exception thrown when a requested resource is not found.
 * Results in HTTP 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
