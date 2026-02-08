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
    public ResponseEntity<List<UserDto>> list() {
        return ResponseEntity.ok(manageUsersUseCase.listAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(manageUsersUseCase.getById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.ok(manageUsersUseCase.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDto> update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(manageUsersUseCase.update(id, request));
    }

    @PatchMapping("/{id}/pin")
    public ResponseEntity<Void> changePosPin(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPin = body.get("pin");
        manageUsersUseCase.changePosPin(id, newPin);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changeBackofficePassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("password");
        manageUsersUseCase.changeBackofficePassword(id, newPassword);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/roles")
    public ResponseEntity<Role[]> listRoles() {
        return ResponseEntity.ok(Role.values());
    }
}