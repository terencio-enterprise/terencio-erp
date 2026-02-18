package es.terencio.erp.auth.domain.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import es.terencio.erp.auth.application.dto.GrantMatrixDto;
import es.terencio.erp.auth.application.dto.ModulePermissionsDto;
import es.terencio.erp.auth.application.dto.PermissionNodeDto;
import es.terencio.erp.auth.application.dto.PermissionSource;
import es.terencio.erp.auth.domain.model.AccessGrant;

@Service
public class PermissionCalculator {

    private final JdbcClient jdbcClient;

    public PermissionCalculator(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public GrantMatrixDto calculateMatrix(Long grantId, AccessGrant grant) {
        // 1. Fetch all permissions
        List<PermissionRow> allPermissions = jdbcClient
                .sql("SELECT code, name, module FROM permissions ORDER BY module, name")
                .query(PermissionRow.class)
                .list();

        // 2. Fetch base permissions for the role
        Set<String> rolePermissions = jdbcClient
                .sql("SELECT permission_code FROM role_permissions WHERE role_name = :role")
                .param("role", grant.role())
                .query(String.class)
                .set();

        // 3. Build Matrix
        Map<String, List<PermissionNodeDto>> byModule = new LinkedHashMap<>();

        // Group permissions by their module to preserve order roughly (though Map
        // iteration depends on implementation, LinkedHashMap preserves insertion)
        // But the list 'allPermissions' is ordered by module, so the loop will process
        // them in order.

        for (PermissionRow p : allPermissions) {
            boolean inRole = rolePermissions.contains(p.code());
            boolean inExtra = grant.extraPermissions() != null && grant.extraPermissions().contains(p.code());
            boolean inExcluded = grant.excludedPermissions() != null && grant.excludedPermissions().contains(p.code());

            boolean value;
            PermissionSource source;

            if (inExcluded) {
                // If explicitly excluded, it is disabled.
                value = false;
                // If it was in role, we mark source as ROLE (indicating it's a disabled role
                // permission)
                // If not in role, but was excluded? Rare case.
                source = inRole ? PermissionSource.ROLE : PermissionSource.EXCLUDED;
            } else {
                if (inExtra) {
                    value = true;
                    source = PermissionSource.EXTRA;
                } else if (inRole) {
                    value = true;
                    source = PermissionSource.ROLE;
                } else {
                    value = false;
                    source = PermissionSource.NONE;
                }
            }

            byModule.computeIfAbsent(p.module(), k -> new ArrayList<>())
                    .add(new PermissionNodeDto(p.code(), p.name(), value, source));
        }

        List<ModulePermissionsDto> modules = byModule.entrySet().stream()
                .map(e -> new ModulePermissionsDto(e.getKey(), e.getValue()))
                .toList();

        return new GrantMatrixDto(grantId, grant.role(), modules);
    }

    public Set<String> calculateEffectivePermissions(AccessGrant grant) {
        // 1. Fetch base permissions for the role
        // In a real high-perf scenario, this should be cached (e.g. @Cacheable)
        Set<String> rolePermissions = jdbcClient
                .sql("SELECT permission_code FROM role_permissions WHERE role_name = :role")
                .param("role", grant.role())
                .query(String.class)
                .set();

        // 2. Apply logic: (Role + Extra) - Excluded
        java.util.Set<String> effective = new java.util.HashSet<>(rolePermissions);
        if (grant.extraPermissions() != null) {
            effective.addAll(grant.extraPermissions());
        }
        if (grant.excludedPermissions() != null) {
            effective.removeAll(grant.excludedPermissions());
        }
        return effective;
    }

    // Helper record
    public static record PermissionRow(String code, String name, String module) {
    }
}
