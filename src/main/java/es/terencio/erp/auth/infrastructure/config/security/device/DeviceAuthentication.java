package es.terencio.erp.auth.infrastructure.config.security.device;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

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

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_DEVICE"));
    }

    @Override public Object getCredentials() { return null; }
    @Override public Object getDetails() { return new DeviceDetails(deviceId, storeId, serialCode); }
    @Override public Object getPrincipal() { return deviceId; }
    @Override public boolean isAuthenticated() { return authenticated; }
    @Override public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException { this.authenticated = isAuthenticated; }
    @Override public String getName() { return serialCode; }

    public UUID getDeviceId() { return deviceId; }
    public UUID getStoreId() { return storeId; }
    public String getSerialCode() { return serialCode; }

    public record DeviceDetails(UUID deviceId, UUID storeId, String serialCode) {}
}
