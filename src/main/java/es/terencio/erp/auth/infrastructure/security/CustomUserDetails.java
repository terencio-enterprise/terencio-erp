package es.terencio.erp.auth.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import es.terencio.erp.auth.domain.model.AccessGrant;
import es.terencio.erp.auth.domain.model.AccessScope;

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
    private final UUID organizationId;
    private final Set<AccessGrant> accessGrants;

    public CustomUserDetails(Long id, UUID uuid, String username, String fullName, String password, String role,
            UUID storeId, UUID companyId, UUID organizationId, Set<AccessGrant> accessGrants) {
        this.id = id;
        this.uuid = uuid;
        this.username = username;
        this.fullName = fullName;
        this.password = password;
        this.role = role;
        this.storeId = storeId;
        this.companyId = companyId;
        this.organizationId = organizationId;
        this.accessGrants = accessGrants == null ? Set.of() : Set.copyOf(accessGrants);
        this.authorities = buildAuthorities(role, this.accessGrants);
    }

    public CustomUserDetails(Long id, UUID uuid, String username, String fullName, String password, String role,
            UUID storeId, UUID companyId) {
        this(id, uuid, username, fullName, password, role, storeId, companyId, null,
                defaultGrants(role, storeId, companyId));
    }

    private static Set<AccessGrant> defaultGrants(String role, UUID storeId, UUID companyId) {
        if (storeId != null) {
            return Set.of(new AccessGrant(AccessScope.STORE, storeId, role));
        }
        if (companyId != null) {
            return Set.of(new AccessGrant(AccessScope.COMPANY, companyId, role));
        }
        return Set.of();
    }

    private Collection<? extends GrantedAuthority> buildAuthorities(String fallbackRole, Set<AccessGrant> grants) {
        if (grants == null || grants.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + fallbackRole));
        }

        Set<SimpleGrantedAuthority> derivedAuthorities = grants.stream()
                .map(AccessGrant::role)
                .filter(r -> r != null && !r.isBlank())
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toSet());

        if (derivedAuthorities.isEmpty()) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + fallbackRole));
        }

        return derivedAuthorities;
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

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Set<AccessGrant> getAccessGrants() {
        return accessGrants;
    }

    public boolean canAccessCompany(UUID targetCompanyId) {
        return accessGrants.stream().anyMatch(grant -> grant.scope() == AccessScope.ORGANIZATION
                || (grant.scope() == AccessScope.COMPANY && grant.targetId().equals(targetCompanyId)));
    }

    public boolean canAccessStore(UUID targetStoreId, UUID parentCompanyId) {
        return accessGrants.stream().anyMatch(grant -> grant.scope() == AccessScope.ORGANIZATION
                || (grant.scope() == AccessScope.COMPANY && grant.targetId().equals(parentCompanyId))
                || (grant.scope() == AccessScope.STORE && grant.targetId().equals(targetStoreId)));
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
