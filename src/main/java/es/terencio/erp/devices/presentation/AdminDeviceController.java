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

    @PutMapping("/{id}/block")
    public ResponseEntity<Void> blockDevice(@PathVariable UUID id) {
        manageDevicesUseCase.blockDevice(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/unblock")
    public ResponseEntity<Void> unblockDevice(@PathVariable UUID id) {
        manageDevicesUseCase.unblockDevice(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate-code")
    public ResponseEntity<GeneratedCodeDto> generateCode(@Valid @RequestBody GenerateCodeRequest request) {
        return ResponseEntity.ok(manageDevicesUseCase.generateRegistrationCode(request));
    }
}