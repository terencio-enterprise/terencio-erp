package es.terencio.erp.auth.infrastructure.config.security;

import java.util.UUID;
import org.springframework.security.core.userdetails.UserDetails;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final UUID uuid;
    private final String username;
    private final String fullName;
    @JsonIgnore
    private final String password;

    public CustomUserDetails(Long id, UUID uuid, String username, String fullName, String password) {
        this.id = id;
        this.uuid = uuid;
        this.username = username;
        this.fullName = fullName;
        this.password = password;
    }

    public Long getId() { return id; }
    public UUID getUuid() { return uuid; }
    public String getFullName() { return fullName; }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
    @Override public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() {
        return java.util.Collections.emptyList();
    }
}
