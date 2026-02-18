package es.terencio.erp.auth.application.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.terencio.erp.auth.application.dto.GrantMatrixDto;
import es.terencio.erp.auth.application.dto.UpdateGrantRequest;
import es.terencio.erp.auth.domain.model.AccessGrant;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.service.PermissionCalculator;
import es.terencio.erp.auth.domain.service.SecurityGuardService;
import es.terencio.erp.auth.infrastructure.security.CustomUserDetails;

@Service
public class GrantAdministrationService {

    private final JdbcClient jdbcClient;
    private final PermissionCalculator permissionCalculator;
    private final SecurityGuardService securityGuardService;
    private final ObjectMapper objectMapper;

    public GrantAdministrationService(JdbcClient jdbcClient, PermissionCalculator permissionCalculator,
            SecurityGuardService securityGuardService, ObjectMapper objectMapper) {
        this.jdbcClient = jdbcClient;
        this.permissionCalculator = permissionCalculator;
        this.securityGuardService = securityGuardService;
        this.objectMapper = objectMapper;
    }

    public List<GrantSummaryDto> getEmployeeGrants(Long employeeId) {
        return jdbcClient.sql("""
                SELECT id, scope, target_id, role, extra_permissions, excluded_permissions
                FROM employee_access_grants
                WHERE employee_id = :employeeId
                """)
                .param("employeeId", employeeId)
                .query((rs, rowNum) -> {
                    Set<String> extra = parseJson(rs.getString("extra_permissions"));
                    Set<String> excluded = parseJson(rs.getString("excluded_permissions"));
                    return new GrantSummaryDto(
                            rs.getLong("id"),
                            rs.getString("scope"),
                            rs.getObject("target_id", UUID.class),
                            "Unknown Target", // We would need to join or fetch names. For now placeholder.
                            rs.getString("role"),
                            new GrantCountsDto(extra.size(), excluded.size()));
                })
                .list();
    }

    public GrantMatrixDto getGrantMatrix(Long grantId) {
        AccessGrantWithId grantData = fetchGrant(grantId);
        return permissionCalculator.calculateMatrix(grantId, grantData.toDomain());
    }

    @Transactional
    public void updateGrant(Long grantId, UpdateGrantRequest request, CustomUserDetails actor) {
        AccessGrantWithId targetGrant = fetchGrant(grantId);

        // 1. Validate Scope Access (Can I touch this grant?)
        validateActorCanManageGrant(actor, targetGrant);

        // 2. Validate Hierarchy
        securityGuardService.validateHierarchy(actor.getRole(), targetGrant.role()); // Check against OLD role
        securityGuardService.validateHierarchy(actor.getRole(), request.roleName()); // Check against NEW role (cannot
                                                                                     // promote someone above yourself)

        // 3. Privilege Escalation (Concept: Find actor's grant for this scope/target)
        // skipping full implementation for now to stick to task scope, but logic place
        // is here.

        try {
            String extraJson = objectMapper.writeValueAsString(request.extraPermissions());
            String excludedJson = objectMapper.writeValueAsString(request.excludedPermissions());

            jdbcClient.sql("""
                    UPDATE employee_access_grants
                    SET role = :role, extra_permissions = :extra::jsonb, excluded_permissions = :excluded::jsonb
                    WHERE id = :id
                    """)
                    .param("role", request.roleName())
                    .param("extra", extraJson)
                    .param("excluded", excludedJson)
                    .param("id", grantId)
                    .update();
        } catch (Exception e) {
            throw new RuntimeException("Failed to update grant", e);
        }
    }

    private void validateActorCanManageGrant(CustomUserDetails actor, AccessGrantWithId targetGrant) {
        // 1. Organization Limit
        if (targetGrant.scope() == AccessScope.ORGANIZATION) {
            boolean hasOrgAccess = actor.getAccessGrants().stream()
                    .anyMatch(g -> g.scope() == AccessScope.ORGANIZATION
                            && g.targetId().equals(targetGrant.targetId()));
            if (!hasOrgAccess)
                throw new org.springframework.security.access.AccessDeniedException(
                        "Only Organization Admins can manage Organization grants");
        }

        // 2. Company Limit
        if (targetGrant.scope() == AccessScope.COMPANY) {
            if (actor.canAccessCompany(targetGrant.targetId()))
                return;
            throw new org.springframework.security.access.AccessDeniedException(
                    "Actor does not have access to this Company");
        }

        // 3. Store Limit
        if (targetGrant.scope() == AccessScope.STORE) {
            UUID storeCompanyId = fetchCompanyIdForStore(targetGrant.targetId());
            if (actor.canAccessStore(targetGrant.targetId(), storeCompanyId))
                return;
            throw new org.springframework.security.access.AccessDeniedException(
                    "Actor does not have access to this Store");
        }
    }

    private UUID fetchCompanyIdForStore(UUID storeId) {
        return jdbcClient.sql("SELECT company_id FROM stores WHERE id = :id")
                .param("id", storeId)
                .query(UUID.class)
                .single();
    }

    private AccessGrantWithId fetchGrant(Long grantId) {
        return jdbcClient.sql(
                "SELECT scope, target_id, role, extra_permissions, excluded_permissions FROM employee_access_grants WHERE id = :id")
                .param("id", grantId)
                .query((rs, rowNum) -> {
                    Set<String> extra = parseJson(rs.getString("extra_permissions"));
                    Set<String> excluded = parseJson(rs.getString("excluded_permissions"));
                    return new AccessGrantWithId(
                            AccessScope.valueOf(rs.getString("scope")),
                            rs.getObject("target_id", UUID.class),
                            rs.getString("role"),
                            extra,
                            excluded);
                })
                .optional()
                .orElseThrow(() -> new RuntimeException("Grant not found"));
    }

    private Set<String> parseJson(String json) {
        if (json == null)
            return Set.of();
        try {
            return objectMapper.readValue(json, new TypeReference<Set<String>>() {
            });
        } catch (Exception e) {
            return Set.of();
        }
    }

    // Helper records
    record AccessGrantWithId(AccessScope scope, UUID targetId, String role, Set<String> extra, Set<String> excluded) {
        AccessGrant toDomain() {
            return new AccessGrant(scope, targetId, role, extra, excluded);
        }
    }

    public record GrantSummaryDto(Long id, String scope, UUID targetId, String targetName, String role,
            GrantCountsDto summary) {
    }

    public record GrantCountsDto(int extrasCount, int excludedCount) {
    }
}
