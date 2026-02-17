package es.terencio.erp.auth.infrastructure.security;

import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import es.terencio.erp.auth.domain.model.AccessGrant;
import es.terencio.erp.auth.domain.model.AccessScope;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final JdbcClient jdbcClient;

    public CustomUserDetailsService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String sql = """
                SELECT id, uuid, username, full_name, password_hash, role, store_id, organization_id
                FROM employees
                    WHERE username = :username AND is_active = TRUE
                """;

        CustomUserDetails userDetails = jdbcClient.sql(sql)
                .param("username", username)
                .query((rs, rowNum) -> {
                    Long employeeId = rs.getLong("id");
                    UUID storeId = rs.getObject("store_id", UUID.class);
                    UUID organizationId = rs.getObject("organization_id", UUID.class);
                    String role = rs.getString("role");

                    var grants = findAccessGrants(employeeId, storeId, organizationId, role);
                    UUID companyId = resolveCompanyId(grants, storeId);

                    return new CustomUserDetails(
                            employeeId,
                            rs.getObject("uuid", UUID.class),
                            rs.getString("username"),
                            rs.getString("full_name"),
                            rs.getString("password_hash"), // Backoffice password
                            role,
                            storeId,
                            companyId,
                            organizationId,
                            grants);
                })
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return userDetails;
    }

    private java.util.Set<AccessGrant> findAccessGrants(Long employeeId, UUID storeId, UUID organizationId,
            String role) {
        var grants = jdbcClient.sql("""
                    SELECT scope, target_id, role
                FROM employee_access_grants
                WHERE employee_id = :employeeId
                    """)
                .param("employeeId", employeeId)
                .query((rs, rowNum) -> new AccessGrant(
                        AccessScope.valueOf(rs.getString("scope")),
                        rs.getObject("target_id", UUID.class),
                        rs.getString("role")))
                .list()
                .stream()
                .collect(Collectors.toSet());

        if (!grants.isEmpty()) {
            return grants;
        }

        if (storeId != null) {
            return java.util.Set.of(new AccessGrant(AccessScope.STORE, storeId, role));
        }

        if (organizationId != null) {
            return java.util.Set.of(new AccessGrant(AccessScope.ORGANIZATION, organizationId, role));
        }

        return java.util.Set.of();
    }

    private UUID resolveCompanyId(java.util.Set<AccessGrant> grants, UUID storeId) {
        UUID companyFromGrant = grants.stream()
                .filter(grant -> grant.scope() == AccessScope.COMPANY)
                .map(AccessGrant::targetId)
                .findFirst()
                .orElse(null);

        if (companyFromGrant != null) {
            return companyFromGrant;
        }

        if (storeId == null) {
            return null;
        }

        return jdbcClient.sql("SELECT company_id FROM stores WHERE id = :storeId")
                .param("storeId", storeId)
                .query((rs, rowNum) -> rs.getObject("company_id", UUID.class))
                .optional()
                .orElse(null);
    }
}