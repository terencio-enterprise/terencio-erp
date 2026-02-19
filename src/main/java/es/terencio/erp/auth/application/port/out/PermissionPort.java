package es.terencio.erp.auth.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import es.terencio.erp.auth.domain.model.AccessScope;

public interface PermissionPort {
    boolean hasPermission(Long employeeId, String permissionCode, UUID targetId, AccessScope scope);
    Map<String, Map<UUID, List<String>>> getPermissionMatrix(Long employeeId);
}
