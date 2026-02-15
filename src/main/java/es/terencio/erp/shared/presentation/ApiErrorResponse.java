package es.terencio.erp.shared.presentation;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard error API response.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private final boolean success = false;
    private final String message;
    private final ErrorDetail error;
    private final Meta meta;

    private ApiErrorResponse(String message, ErrorDetail error, Meta meta) {
        this.message = message;
        this.error = error;
        this.meta = meta;
    }

    public static ApiErrorResponse error(String message, String code) {
        return new ApiErrorResponse(
                message,
                new ErrorDetail(code, null),
                new Meta(Instant.now()));
    }

    public static ApiErrorResponse error(String message, String code, List<FieldError> details) {
        return new ApiErrorResponse(
                message,
                new ErrorDetail(code, details),
                new Meta(Instant.now()));
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public ErrorDetail getError() {
        return error;
    }

    public Meta getMeta() {
        return meta;
    }

    public record Meta(Instant timestamp) {
    }

    public record ErrorDetail(String code, List<FieldError> details) {
    }

    public record FieldError(String field, String issue) {
    }
}
