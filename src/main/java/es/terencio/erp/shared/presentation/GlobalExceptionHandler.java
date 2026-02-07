package es.terencio.erp.shared.presentation;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.terencio.erp.shared.exception.DomainException;
import es.terencio.erp.shared.exception.RegistrationException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses across the application.
 */
@Slf4j // Adds the 'log' object automatically (Lombok)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ErrorResponse> handleRegistrationException(RegistrationException ex) {
        // Log at WARN level because it's usually a user error (wrong code, expired,
        // etc.)
        log.warn("Registration failed: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(DomainException ex) {
        log.warn("Domain rule violation: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                Instant.now());
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Log basic validation failure info
        log.warn("Validation failed for request: {}", ex.getBindingResult().getObjectName());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ValidationErrorResponse response = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Validation failed",
                Instant.now(),
                errors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // CRITICAL: Log the full stack trace for unexpected 500 errors
        log.error("Unexpected system error occurred", ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected error occurred",
                Instant.now());
        return ResponseEntity.internalServerError().body(error);
    }

    /**
     * Standard error response.
     */
    public record ErrorResponse(
            int status,
            String message,
            Instant timestamp) {
    }

    /**
     * Validation error response with field details.
     */
    public record ValidationErrorResponse(
            int status,
            String message,
            Instant timestamp,
            Map<String, String> errors) {
    }
}