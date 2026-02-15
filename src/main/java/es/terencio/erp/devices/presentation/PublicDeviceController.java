package es.terencio.erp.devices.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.devices.application.dto.SetupConfirmRequest;
import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupPreviewRequest;
import es.terencio.erp.devices.application.dto.SetupResultDto;
import es.terencio.erp.devices.application.port.in.SetupDeviceUseCase;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/devices/setup")
public class PublicDeviceController {

    private final SetupDeviceUseCase setupDeviceUseCase;

    public PublicDeviceController(SetupDeviceUseCase setupDeviceUseCase) {
        this.setupDeviceUseCase = setupDeviceUseCase;
    }

    @PostMapping("/preview")
    public ResponseEntity<SetupPreviewDto> preview(@Valid @RequestBody SetupPreviewRequest request) {
        return ResponseEntity.ok(setupDeviceUseCase.previewSetup(request.code()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<SetupResultDto> confirm(@Valid @RequestBody SetupConfirmRequest request) {
        return ResponseEntity.ok(setupDeviceUseCase.confirmSetup(request.code(), request.hardwareId()));
    }
}