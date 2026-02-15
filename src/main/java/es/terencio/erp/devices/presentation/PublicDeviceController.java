package es.terencio.erp.devices.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupResultDto;
import es.terencio.erp.devices.application.port.in.SetupDeviceUseCase;

/**
 * Public endpoints for device registration (no authentication required).
 */
@RestController
@RequestMapping("/api/v1/public/devices")
public class PublicDeviceController {

    private final SetupDeviceUseCase setupDeviceUseCase;

    public PublicDeviceController(SetupDeviceUseCase setupDeviceUseCase) {
        this.setupDeviceUseCase = setupDeviceUseCase;
    }

    @GetMapping("/preview/{code}")
    public ResponseEntity<SetupPreviewDto> preview(@PathVariable String code) {
        return ResponseEntity.ok(setupDeviceUseCase.previewSetup(code));
    }

    @PostMapping("/confirm/{code}")
    public ResponseEntity<SetupResultDto> confirm(
            @PathVariable String code,
            @RequestParam String hardwareId) {
        return ResponseEntity.ok(setupDeviceUseCase.confirmSetup(code, hardwareId));
    }
}