package es.terencio.erp.auth.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final UUID uuid;
    private final String username;
    private final String fullName;
    private final String role;
    @JsonIgnore
    private final String password; // This maps to pin_hash in DB
    private final Collection<? extends GrantedAuthority> authorities;
    private final UUID storeId;
    private final UUID companyId;

    public CustomUserDetails(Long id, UUID uuid, String username, String fullName, String password, String role,
            UUID storeId, UUID companyId) {
        this.id = id;
        this.uuid = uuid;
        this.username = username;
        this.fullName = fullName;
        this.password = password;
        this.role = role;
        this.storeId = storeId;
        this.companyId = companyId;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public Long getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public UUID getStoreId() {
        return storeId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}