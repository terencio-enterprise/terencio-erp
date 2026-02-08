package es.terencio.erp.devices.presentation;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import es.terencio.erp.devices.application.dto.*;
import es.terencio.erp.devices.application.port.in.ManageDevicesUseCase;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin/devices")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeviceController {

    private final ManageDevicesUseCase manageDevicesUseCase;

    public AdminDeviceController(ManageDevicesUseCase manageDevicesUseCase) {
        this.manageDevicesUseCase = manageDevicesUseCase;
    }

    @GetMapping
    public ResponseEntity<List<DeviceDto>> list() {
        return ResponseEntity.ok(manageDevicesUseCase.listAll());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestParam String action) {
        if ("block".equalsIgnoreCase(action)) manageDevicesUseCase.blockDevice(id);
        else if ("unblock".equalsIgnoreCase(action)) manageDevicesUseCase.unblockDevice(id);
        else return ResponseEntity.badRequest().build();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/codes")
    public ResponseEntity<GeneratedCodeDto> generateCode(@Valid @RequestBody GenerateCodeRequest request) {
        return ResponseEntity.ok(manageDevicesUseCase.generateRegistrationCode(request));
    }
}