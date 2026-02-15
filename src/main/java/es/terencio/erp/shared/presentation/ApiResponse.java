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

    private final boolean success = true;
    private final String message;
    private final T data;
    private final Meta meta;

    private ApiResponse(String message, T data, Meta meta) {
        this.message = message;
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data, new Meta(Instant.now()));
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("Operation successful", data, new Meta(Instant.now()));
    }

    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(message, null, new Meta(Instant.now()));
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

    public Meta getMeta() {
        return meta;
    }

    public record Meta(Instant timestamp) {
    }
}
