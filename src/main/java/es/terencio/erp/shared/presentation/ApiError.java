package es.terencio.erp.shared.presentation;

import java.util.List;

/**
 * Standard error structure for API responses.
 * 
 * @param code    A machine-readable error code (e.g., "VALIDATION_ERROR").
 * @param details A list of specific error details (optional).
 */
public record ApiError(
        String code,
        String message,
        List<ErrorDetail> details) {

    public record ErrorDetail(
            String field,
            String message) {
    }

    @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING)
    public ApiError(String message) {
        this("UNKNOWN_ERROR", message, null);
    }
}
