package es.terencio.erp.auth.application.dto;

public record LoginResponse(
        String token,
        String type,
        String username) {
    public LoginResponse(String token, String username) {
        this(token, "Bearer", username);
    }
}
