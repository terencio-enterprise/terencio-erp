package es.terencio.erp.auth.application.dto;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import es.terencio.erp.organization.application.dto.OrganizationDtos.CompanyTreeDto;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(@NotBlank(message = "Username is required") String username, @NotBlank(message = "Password/PIN is required") String password) {}
    
    public record LoginResponse(String token, String type, String username) {
        public LoginResponse(String token, String username) {
            this(token, "Bearer", username);
        }
    }

    public record EmployeeInfoDto(Long id, String username, String fullName, boolean isActive, UUID lastCompanyId, UUID lastStoreId, List<CompanyTreeDto> companies, Map<String, Map<UUID, List<String>>> permissions) {}
    
    public record GrantMatrixDto(Long grantId, String roleName, List<ModulePermissionsDto> modules) {}
    
    public record ModulePermissionsDto(String moduleName, List<PermissionNodeDto> permissions) {}
    
    public record PermissionNodeDto(String code, String name, boolean value, PermissionSource source) {}
    
    public record UpdateGrantRequest(String roleName, Set<String> extraPermissions, Set<String> excludedPermissions) {}
}
