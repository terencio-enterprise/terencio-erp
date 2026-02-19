package es.terencio.erp.devices.infrastructure.in.web;

import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.auth.domain.model.AccessScope;
import es.terencio.erp.auth.domain.model.Permission;
import es.terencio.erp.auth.infrastructure.config.security.aop.RequiresPermission;
import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.dto.GenerateCodeRequest;
import es.terencio.erp.devices.application.dto.GeneratedCodeDto;
import es.terencio.erp.devices.application.port.in.ManageDevicesUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/devices")
@Tag(name = "Admin Devices", description = "Administrative management of POS devices")
public class AdminDeviceController {

    private final ManageDevicesUseCase manageDevicesUseCase;

    public AdminDeviceController(ManageDevicesUseCase manageDevicesUseCase) {
        this.manageDevicesUseCase = manageDevicesUseCase;
    }

    @GetMapping
    @Operation(summary = "List devices")
    @RequiresPermission(permission = Permission.DEVICE_VIEW, scope = AccessScope.STORE, targetIdParam = "storeId")
    public ResponseEntity<ApiResponse<List<DeviceDto>>> list(@RequestParam UUID storeId) {
        return ResponseEntity.ok(ApiResponse.success("Devices fetched successfully", manageDevicesUseCase.listAll()));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block device")
    @RequiresPermission(permission = Permission.DEVICE_MANAGE, scope = AccessScope.STORE, targetIdParam = "storeId")
    public ResponseEntity<ApiResponse<Void>> blockDevice(@PathVariable UUID id, @RequestParam UUID storeId) {
        manageDevicesUseCase.blockDevice(id);
        return ResponseEntity.ok(ApiResponse.success("Device blocked successfully"));
    }

    @PostMapping("/generate-code")
    @Operation(summary = "Generate registration code")
    @RequiresPermission(permission = Permission.DEVICE_MANAGE, scope = AccessScope.STORE, targetIdParam = "storeId")
    public ResponseEntity<ApiResponse<GeneratedCodeDto>> generateCode(@RequestParam UUID storeId, @Valid @RequestBody GenerateCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Registration code generated", manageDevicesUseCase.generateRegistrationCode(request)));
    }
}
