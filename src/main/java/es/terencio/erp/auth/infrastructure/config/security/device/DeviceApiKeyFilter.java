package es.terencio.erp.auth.infrastructure.config.security.device;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import es.terencio.erp.devices.infrastructure.security.DeviceApiKeyGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class DeviceApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final DeviceApiKeyGenerator apiKeyGenerator;
    private final DataSource dataSource;

    public DeviceApiKeyFilter(DeviceApiKeyGenerator apiKeyGenerator, DataSource dataSource) {
        this.apiKeyGenerator = apiKeyGenerator;
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader(API_KEY_HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            UUID deviceId = apiKeyGenerator.extractDeviceId(apiKey);
            if (deviceId == null) {
                filterChain.doFilter(request, response);
                return;
            }

            DeviceInfo deviceInfo = loadDeviceInfo(deviceId);
            if (deviceInfo == null) {
                filterChain.doFilter(request, response);
                return;
            }

            if ("BLOCKED".equals(deviceInfo.status)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Device is blocked\"}");
                return;
            }

            if (!"ACTIVE".equals(deviceInfo.status)) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean isValid = apiKeyGenerator.validateApiKey(apiKey, deviceId, deviceInfo.deviceSecret, deviceInfo.apiKeyVersion);

            if (isValid) {
                DeviceAuthentication authentication = new DeviceAuthentication(deviceId, deviceInfo.storeId, "DEVICE-" + deviceId);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                updateLastAuthenticated(deviceId);
            }
        } catch (Exception e) {
            logger.error("Error validating device API key", e);
        }

        filterChain.doFilter(request, response);
    }

    private DeviceInfo loadDeviceInfo(UUID deviceId) {
        String sql = "SELECT device_secret, api_key_version, status, store_id FROM devices WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, deviceId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new DeviceInfo(rs.getString("device_secret"), rs.getInt("api_key_version"), rs.getString("status"), (UUID) rs.getObject("store_id"));
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private void updateLastAuthenticated(UUID deviceId) {
        String sql = "UPDATE devices SET last_authenticated_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, java.sql.Timestamp.from(Instant.now()));
            stmt.setObject(2, deviceId);
            stmt.executeUpdate();
        } catch (Exception e) {}
    }

    private record DeviceInfo(String deviceSecret, int apiKeyVersion, String status, UUID storeId) {}
}
