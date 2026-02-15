package es.terencio.erp.auth.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Custom authentication object for POS devices authenticated via API key.
 * This is different from user JWT authentication.
 */
public class DeviceAuthentication implements Authentication {

    private final UUID deviceId;
    private final UUID storeId;
    private final String serialCode;
    private boolean authenticated = true;

    public DeviceAuthentication(UUID deviceId, UUID storeId, String serialCode) {
        this.deviceId = deviceId;
        this.storeId = storeId;
        this.serialCode = serialCode;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Devices have a special DEVICE role
        return List.of(new SimpleGrantedAuthority("ROLE_DEVICE"));
    }

    @Override
    public Object getCredentials() {
        return null; // API key not stored in memory after validation
    }

    @Override
    public Object getDetails() {
        return new DeviceDetails(deviceId, storeId, serialCode);
    }

    @Override
    public Object getPrincipal() {
        return deviceId; // Device ID is the principal
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return serialCode; // Display name
    }

    public UUID getDeviceId() {
        return deviceId;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public String getSerialCode() {
        return serialCode;
    }

    /**
     * Details object containing device information
     */
    public record DeviceDetails(UUID deviceId, UUID storeId, String serialCode) {
    }
}
