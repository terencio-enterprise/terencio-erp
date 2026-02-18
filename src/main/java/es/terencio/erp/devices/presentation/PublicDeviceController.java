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
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Public endpoints for device registration (no authentication required).
 */
@RestController
@RequestMapping("/api/v1/public/devices")
@Tag(name = "Public Devices", description = "Public endpoints for device registration and setup")
public class PublicDeviceController {

    private final SetupDeviceUseCase setupDeviceUseCase;

    public PublicDeviceController(SetupDeviceUseCase setupDeviceUseCase) {
        this.setupDeviceUseCase = setupDeviceUseCase;
    }

    @GetMapping("/preview/{code}")
    @Operation(summary = "Preview setup", description = "Validates registration code and returns setup preview")
    public ResponseEntity<ApiResponse<SetupPreviewDto>> preview(
            @Parameter(description = "Registration code") @PathVariable String code) {
        return ResponseEntity
                .ok(ApiResponse.success("Setup preview fetched successfully", setupDeviceUseCase.previewSetup(code)));
    }

    @PostMapping("/confirm/{code}")
    @Operation(summary = "Confirm setup", description = "Confirms device setup with registration code and hardware identifier")
    public ResponseEntity<ApiResponse<SetupResultDto>> confirm(
            @Parameter(description = "Registration code") @PathVariable String code,
            @Parameter(description = "Hardware identifier") @RequestParam String hardwareId) {
        return ResponseEntity.ok(ApiResponse.success("Device setup confirmed successfully",
                setupDeviceUseCase.confirmSetup(code, hardwareId)));
    }
}
