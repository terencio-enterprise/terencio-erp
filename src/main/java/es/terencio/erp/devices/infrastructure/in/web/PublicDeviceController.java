package es.terencio.erp.devices.infrastructure.in.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.devices.application.dto.SetupPreviewDto;
import es.terencio.erp.devices.application.dto.SetupResultDto;
import es.terencio.erp.devices.application.port.in.SetupDeviceUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/public/devices")
@Tag(name = "Public Devices", description = "Public endpoints for device registration")
public class PublicDeviceController {

    private final SetupDeviceUseCase setupDeviceUseCase;

    public PublicDeviceController(SetupDeviceUseCase setupDeviceUseCase) {
        this.setupDeviceUseCase = setupDeviceUseCase;
    }

    @GetMapping("/preview/{code}")
    @Operation(summary = "Preview setup")
    public ResponseEntity<ApiResponse<SetupPreviewDto>> preview(@PathVariable String code) {
        return ResponseEntity.ok(ApiResponse.success("Setup preview fetched", setupDeviceUseCase.previewSetup(code)));
    }

    @PostMapping("/confirm/{code}")
    @Operation(summary = "Confirm setup")
    public ResponseEntity<ApiResponse<SetupResultDto>> confirm(@PathVariable String code, @RequestParam String hardwareId) {
        return ResponseEntity.ok(ApiResponse.success("Device setup confirmed", setupDeviceUseCase.confirmSetup(code, hardwareId)));
    }
}
