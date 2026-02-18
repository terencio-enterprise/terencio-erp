package es.terencio.erp.auth.domain.service;

import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class SecurityGuardService {

    private static final Map<String, Integer> ROLE_HIERARCHY = Map.of(
            "OWNER", 100,
            "ADMIN", 80,
            "MANAGER", 50,
            "CASHIER", 10,
            "WAREHOUSE", 10);

    public void validateHierarchy(String actorRole, String targetRole) {
        int actorRank = getRank(actorRole);
        int targetRank = getRank(targetRole);

        if (actorRank <= targetRank) {
            throw new AccessDeniedException(
                    "Insufficient privileges: Cannot modify permissions for a user with equal or higher rank ("
                            + targetRole + ")");
        }
    }

    public void validatePrivilegeEscalation(Set<String> actorPermissions, Set<String> requestedChanges) {
        // "El actor solo puede ver y asignar permisos que él mismo posea en el contexto
        // actual."
        // Check if any requested change (add extra) is NOT in actor permissions.
        // We assume actorPermissions contains ALL their effective permissions.

        for (String perm : requestedChanges) {
            if (!actorPermissions.contains(perm)) {
                throw new AccessDeniedException(
                        "Privilege Escalation Detected: You cannot grant a permission you do not possess (" + perm
                                + ")");
            }
        }
    }

    private int getRank(String role) {
        return ROLE_HIERARCHY.getOrDefault(role, 0);
    }
}
