package es.terencio.erp.employees.infrastructure.in.web;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.employees.application.dto.CreateEmployeeRequest;
import es.terencio.erp.employees.application.dto.EmployeeDto;
import es.terencio.erp.employees.application.dto.UpdateEmployeeRequest;
import es.terencio.erp.employees.application.port.in.ManageEmployeesUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/employees")
@Tag(name = "Admin Employees", description = "Administrative employee management endpoints")
public class AdminEmployeeController {

    private final ManageEmployeesUseCase manageEmployeesUseCase;

    public AdminEmployeeController(ManageEmployeesUseCase manageEmployeesUseCase) {
        this.manageEmployeesUseCase = manageEmployeesUseCase;
    }

    @GetMapping
    @Operation(summary = "List employees")
    @RequiresPermission(permission = Permission.EMPLOYEE_VIEW, scope = AccessScope.ORGANIZATION, targetIdParam = "organizationId")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> list(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.success("Employees fetched", manageEmployeesUseCase.listAll()));
    }

    @PostMapping
    @Operation(summary = "Create employee")
    @RequiresPermission(permission = Permission.EMPLOYEE_CREATE, scope = AccessScope.ORGANIZATION, targetIdParam = "organizationId")
    public ResponseEntity<ApiResponse<EmployeeDto>> create(@RequestParam UUID organizationId, @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Employee created", manageEmployeesUseCase.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee")
    @RequiresPermission(permission = Permission.EMPLOYEE_UPDATE, scope = AccessScope.ORGANIZATION, targetIdParam = "organizationId")
    public ResponseEntity<ApiResponse<EmployeeDto>> update(@RequestParam UUID organizationId, @PathVariable Long id, @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Employee updated", manageEmployeesUseCase.update(id, request)));
    }
}
