package es.terencio.erp.auth.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import es.terencio.erp.auth.application.port.out.PermissionPort;
import es.terencio.erp.auth.domain.model.AccessScope;

@Service
public class PermissionService {

    private final PermissionPort permissionPort;

    public PermissionService(PermissionPort permissionPort) {
        this.permissionPort = permissionPort;
    }

    public boolean hasPermission(Long employeeId, String permissionCode, UUID targetId, AccessScope scope) {
        return permissionPort.hasPermission(employeeId, permissionCode, targetId, scope);
    }

    public Map<String, Map<UUID, List<String>>> getPermissionMatrix(Long employeeId) {
        return permissionPort.getPermissionMatrix(employeeId);
    }
}
