package es.terencio.erp.shared.presentation;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard successful API response wrapper.
 * 
 * @param <T> The type of data being returned
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String message;
    private final T data;
    private final ApiError error;
    private final Meta meta;

    private ApiResponse(boolean success, String message, T data, ApiError error, Meta meta) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.error = error;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null, new Meta(Instant.now()));
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Operation successful", data, null, new Meta(Instant.now()));
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, null, new Meta(Instant.now()));
    }

    public static <T> ApiResponse<T> error(String message, ApiError error) {
        return new ApiResponse<>(false, message, null, error, new Meta(Instant.now()));
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiError error = new ApiError("UNKNOWN_ERROR", message, null);
        return new ApiResponse<>(false, message, null, error, new Meta(Instant.now()));
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ApiError getError() {
        return error;
    }

    public Meta getMeta() {
        return meta;
    }

    public record Meta(Instant timestamp) {
    }
}
