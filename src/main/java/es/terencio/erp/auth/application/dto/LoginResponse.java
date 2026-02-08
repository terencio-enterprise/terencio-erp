package es.terencio.erp.auth.application.dto;

public record LoginResponse(
    String token,
    String type,
    String username,
    String role
) {
    public LoginResponse(String token, String username, String role) {
        this(token, "Bearer", username, role);
    }
}