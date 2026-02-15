package es.terencio.erp.shared.presentation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import es.terencio.erp.shared.exception.DomainException;
import es.terencio.erp.shared.exception.RegistrationException;
import es.terencio.erp.shared.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses across the application.
 */
@Slf4j // Adds the 'log' object automatically (Lombok)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ApiErrorResponse error = ApiErrorResponse.error(
                ex.getMessage(),
                "RESOURCE_NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ApiErrorResponse> handleRegistrationException(RegistrationException ex) {
        // Log at WARN level because it's usually a user error (wrong code, expired,
        // etc.)
        log.warn("Registration failed: {}", ex.getMessage());

        ApiErrorResponse error = ApiErrorResponse.error(
                ex.getMessage(),
                "REGISTRATION_ERROR");
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex) {
        log.warn("Domain rule violation: {}", ex.getMessage());

        ApiErrorResponse error = ApiErrorResponse.error(
                ex.getMessage(),
                "DOMAIN_ERROR");
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        // Log basic validation failure info
        log.warn("Validation failed for request: {}", ex.getBindingResult().getObjectName());

        List<ApiErrorResponse.FieldError> fieldErrors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.add(new ApiErrorResponse.FieldError(fieldName, errorMessage));
        });

        ApiErrorResponse response = ApiErrorResponse.error(
                "Validation failed",
                "VALIDATION_ERROR",
                fieldErrors);

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Authentication failed: bad credentials");

        ApiErrorResponse error = ApiErrorResponse.error(
                "Invalid username or password",
                "BAD_CREDENTIALS");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountStatusException(AccountStatusException ex) {
        log.warn("Authentication failed: account status issue - {}", ex.getMessage());

        ApiErrorResponse error = ApiErrorResponse.error(
                "Account is disabled or locked",
                "ACCOUNT_STATUS_ERROR");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ApiErrorResponse error = ApiErrorResponse.error(
                "Authentication failed",
                "AUTHENTICATION_ERROR");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex) {
        // CRITICAL: Log the full stack trace for unexpected 500 errors
        log.error("Unexpected system error occurred", ex);

        ApiErrorResponse error = ApiErrorResponse.error(
                "An unexpected error occurred",
                "INTERNAL_SERVER_ERROR");
        return ResponseEntity.internalServerError().body(error);
    }
}