package es.terencio.erp.devices.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Devices", description = "Administrative management of POS devices")
public class AdminDeviceController {

    private final ManageDevicesUseCase manageDevicesUseCase;

    public AdminDeviceController(ManageDevicesUseCase manageDevicesUseCase) {
        this.manageDevicesUseCase = manageDevicesUseCase;
    }

    @GetMapping
    @Operation(summary = "List devices", description = "Returns all registered devices")
    public ResponseEntity<ApiResponse<List<DeviceDto>>> list() {
        return ResponseEntity.ok(ApiResponse.success("Devices fetched successfully", manageDevicesUseCase.listAll()));
    }

    @PutMapping("/{id}/block")
    @Operation(summary = "Block device", description = "Blocks a device and disables access")
    public ResponseEntity<ApiResponse<Void>> blockDevice(@PathVariable UUID id) {
        manageDevicesUseCase.blockDevice(id);
        return ResponseEntity.ok(ApiResponse.success("Device blocked successfully"));
    }

    @PutMapping("/{id}/unblock")
    @Operation(summary = "Unblock device", description = "Unblocks a previously blocked device")
    public ResponseEntity<ApiResponse<Void>> unblockDevice(@PathVariable UUID id) {
        manageDevicesUseCase.unblockDevice(id);
        return ResponseEntity.ok(ApiResponse.success("Device unblocked successfully"));
    }

    @PostMapping("/generate-code")
    @Operation(summary = "Generate registration code", description = "Generates a one-time code to register a device")
    public ResponseEntity<ApiResponse<GeneratedCodeDto>> generateCode(@Valid @RequestBody GenerateCodeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Registration code generated successfully",
                manageDevicesUseCase.generateRegistrationCode(request)));
    }
}