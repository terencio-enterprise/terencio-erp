package es.terencio.erp.users.presentation;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.shared.presentation.ApiResponse;
import es.terencio.erp.users.application.dto.CreateUserRequest;
import es.terencio.erp.users.application.dto.UpdateUserRequest;
import es.terencio.erp.users.application.dto.UserDto;
import es.terencio.erp.users.application.port.in.ManageUsersUseCase;
import es.terencio.erp.users.domain.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Users", description = "Administrative user management endpoints")
public class AdminUserController {

    private final ManageUsersUseCase manageUsersUseCase;

    public AdminUserController(ManageUsersUseCase manageUsersUseCase) {
        this.manageUsersUseCase = manageUsersUseCase;
    }

    @GetMapping
    @Operation(summary = "List users", description = "Returns all users")
    public ResponseEntity<ApiResponse<List<UserDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", manageUsersUseCase.listAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user", description = "Returns one user by identifier")
    public ResponseEntity<ApiResponse<UserDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", manageUsersUseCase.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Creates a new system user")
    public ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User created successfully", manageUsersUseCase.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user", description = "Updates an existing user")
    public ResponseEntity<ApiResponse<UserDto>> update(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("User updated successfully", manageUsersUseCase.update(id, request)));
    }

    @PatchMapping("/{id}/pin")
    @Operation(summary = "Change POS PIN", description = "Changes POS PIN for a user")
    public ResponseEntity<ApiResponse<Void>> changePosPin(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        manageUsersUseCase.changePosPin(id, body.get("pin"));
        return ResponseEntity.ok(ApiResponse.success("POS PIN changed successfully"));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Change password", description = "Changes backoffice password for a user")
    public ResponseEntity<ApiResponse<Void>> changeBackofficePassword(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        manageUsersUseCase.changeBackofficePassword(id, body.get("password"));
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/roles")
    @Operation(summary = "List roles", description = "Returns all available user roles")
    public ResponseEntity<ApiResponse<Role[]>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success("Roles fetched successfully", Role.values()));
    }
}