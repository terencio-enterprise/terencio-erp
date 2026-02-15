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
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final ManageUsersUseCase manageUsersUseCase;

    public AdminUserController(ManageUsersUseCase manageUsersUseCase) {
        this.manageUsersUseCase = manageUsersUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Users fetched successfully", manageUsersUseCase.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("User fetched successfully", manageUsersUseCase.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User created successfully", manageUsersUseCase.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> update(@PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("User updated successfully", manageUsersUseCase.update(id, request)));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<Void>> changePosPin(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        manageUsersUseCase.changePosPin(id, body.get("pin"));
        return ResponseEntity.ok(ApiResponse.success("POS PIN changed successfully"));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changeBackofficePassword(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        manageUsersUseCase.changeBackofficePassword(id, body.get("password"));
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }

    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<Role[]>> listRoles() {
        return ResponseEntity.ok(ApiResponse.success("Roles fetched successfully", Role.values()));
    }
}