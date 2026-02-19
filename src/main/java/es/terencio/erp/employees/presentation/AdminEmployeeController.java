package es.terencio.erp.employees.presentation;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "List employees", description = "Returns all employees")
    public ResponseEntity<ApiResponse<List<EmployeeDto>>> list() {
        return ResponseEntity
                .ok(ApiResponse.success("Employees fetched successfully", manageEmployeesUseCase.listAll()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee", description = "Returns one employee by identifier")
    public ResponseEntity<ApiResponse<EmployeeDto>> get(@PathVariable Long id) {
        return ResponseEntity
                .ok(ApiResponse.success("Employee fetched successfully", manageEmployeesUseCase.getById(id)));
    }

    @PostMapping
    @Operation(summary = "Create employee", description = "Creates a new employee")
    public ResponseEntity<ApiResponse<EmployeeDto>> create(@Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Employee created successfully", manageEmployeesUseCase.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update employee", description = "Updates an existing employee")
    public ResponseEntity<ApiResponse<EmployeeDto>> update(@PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity
                .ok(ApiResponse.success("Employee updated successfully", manageEmployeesUseCase.update(id, request)));
    }

    @PatchMapping("/{id}/pin")
    @Operation(summary = "Change POS PIN", description = "Changes POS PIN for an employee")
    public ResponseEntity<ApiResponse<Void>> changePosPin(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        manageEmployeesUseCase.changePosPin(id, body.get("pin"));
        return ResponseEntity.ok(ApiResponse.success("POS PIN changed successfully"));
    }

    @PatchMapping("/{id}/password")
    @Operation(summary = "Change password", description = "Changes backoffice password for an employee")
    public ResponseEntity<ApiResponse<Void>> changeBackofficePassword(@PathVariable Long id,
            @RequestBody Map<String, String> body) {
        manageEmployeesUseCase.changeBackofficePassword(id, body.get("password"));
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
