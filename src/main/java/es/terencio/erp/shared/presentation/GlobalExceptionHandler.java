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
import es.terencio.erp.shared.presentation.ApiError.ErrorDetail;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler for REST controllers.
 * Provides consistent error responses across the application.
 */
@Slf4j // Adds the 'log' object automatically (Lombok)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ApiError error = new ApiError("RESOURCE_NOT_FOUND", ex.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(RegistrationException.class)
    public ResponseEntity<ApiResponse<Void>> handleRegistrationException(RegistrationException ex) {
        log.warn("Registration failed: {}", ex.getMessage());

        ApiError error = new ApiError("REGISTRATION_ERROR", ex.getMessage(), null);
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainException(DomainException ex) {
        log.warn("Domain rule violation: {}", ex.getMessage());

        ApiError error = new ApiError("DOMAIN_ERROR", ex.getMessage(), null);
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage(), error));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.warn("Validation failed for request: {}", ex.getBindingResult().getObjectName());

        List<ErrorDetail> details = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((org.springframework.validation.FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.add(new ApiError.ErrorDetail(fieldName, errorMessage));
        });

        ApiError error = new ApiError("VALIDATION_ERROR", "One or more inputs are invalid", details);

        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", error));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Authentication failed: bad credentials");

        ApiError error = new ApiError("BAD_CREDENTIALS", "Invalid username or password", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Authentication failed", error));
    }

    @ExceptionHandler(AccountStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountStatusException(AccountStatusException ex) {
        log.warn("Authentication failed: account status issue - {}", ex.getMessage());

        ApiError error = new ApiError("ACCOUNT_STATUS_ERROR", "Account is disabled or locked", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Authentication failed", error));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        ApiError error = new ApiError("AUTHENTICATION_ERROR", "Authentication failed", null);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Authentication failed", error));
    }

    @ExceptionHandler({ org.springframework.security.access.AccessDeniedException.class,
            org.springframework.security.authorization.AuthorizationDeniedException.class })
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(RuntimeException ex) {
        log.warn("Access denied: {}", ex.getMessage());

        ApiError error = new ApiError("ACCESS_DENIED", "Access denied", null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied", error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected system error occurred", ex);

        ApiError error = new ApiError("INTERNAL_SERVER_ERROR", "An unexpected error occurred", null);
        return ResponseEntity.internalServerError().body(ApiResponse.error("An unexpected error occurred", error));
    }
}
