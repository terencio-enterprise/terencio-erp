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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private final es.terencio.erp.auth.domain.service.PermissionCalculator permissionCalculator;

    public CustomUserDetailsService(JdbcClient jdbcClient, com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            es.terencio.erp.auth.domain.service.PermissionCalculator permissionCalculator) {
        this.jdbcClient = jdbcClient;
        this.objectMapper = objectMapper;
        this.permissionCalculator = permissionCalculator;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Removed store_id from query as it is dropped in V4
        String sql = """
                SELECT id, uuid, username, full_name, password_hash, role, organization_id
                FROM employees
                    WHERE username = :username AND is_active = TRUE
                """;

        CustomUserDetails userDetails = jdbcClient.sql(sql)
                .param("username", username)
                .query((rs, rowNum) -> {
                    Long employeeId = rs.getLong("id");
                    UUID organizationId = rs.getObject("organization_id", UUID.class);
                    String role = rs.getString("role");

                    // 1. Fetch grants purely from DB (no legacy store_id fallback)
                    var grants = findAccessGrants(employeeId, organizationId, role);

                    // 2. Derive context from grants
                    // Primary Store ID: Arbitrarily pick the first STORE scope grant, or null
                    UUID storeId = grants.stream()
                            .filter(g -> g.scope() == AccessScope.STORE)
                            .map(AccessGrant::targetId)
                            .findFirst()
                            .orElse(null);

                    // Primary Company ID: Company grant OR resolved from Store
                    UUID companyId = resolveCompanyId(grants, storeId);

                    // 3. Calculate effective permissions
                    java.util.Set<es.terencio.erp.auth.domain.model.PermissionContext> effectivePermissions = new java.util.HashSet<>();
                    for (AccessGrant grant : grants) {
                        java.util.Set<String> perms = permissionCalculator.calculateEffectivePermissions(grant);
                        for (String p : perms) {
                            effectivePermissions.add(new es.terencio.erp.auth.domain.model.PermissionContext(
                                    grant.scope(),
                                    grant.targetId(),
                                    p));
                        }
                    }

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
                            grants,
                            effectivePermissions);
                })
                .optional()
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return userDetails;
    }

    private java.util.Set<AccessGrant> findAccessGrants(Long employeeId, UUID organizationId, String role) {
        var grants = jdbcClient.sql("""
                    SELECT scope, target_id, role, extra_permissions, excluded_permissions
                FROM employee_access_grants
                WHERE employee_id = :employeeId
                    """)
                .param("employeeId", employeeId)
                .query((rs, rowNum) -> {
                    String extraJson = rs.getString("extra_permissions");
                    String excludedJson = rs.getString("excluded_permissions");
                    java.util.Set<String> extra = java.util.Set.of();
                    java.util.Set<String> excluded = java.util.Set.of();
                    try {
                        if (extraJson != null)
                            extra = objectMapper.readValue(extraJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Set<String>>() {
                                    });
                        if (excludedJson != null)
                            excluded = objectMapper.readValue(excludedJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Set<String>>() {
                                    });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return new AccessGrant(
                            AccessScope.valueOf(rs.getString("scope")),
                            rs.getObject("target_id", UUID.class),
                            rs.getString("role"),
                            extra,
                            excluded);
                })
                .list()
                .stream()
                .collect(Collectors.toSet());

        if (!grants.isEmpty()) {
            return grants;
        }

        // Fallback for purely Organization-level users (if no explicit grants exist yet
        // but OrganizationID is on employee)
        if (organizationId != null) {
            return java.util.Set.of(new AccessGrant(AccessScope.ORGANIZATION, organizationId, role, java.util.Set.of(),
                    java.util.Set.of()));
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
