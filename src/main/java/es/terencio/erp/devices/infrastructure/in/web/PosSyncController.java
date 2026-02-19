package es.terencio.erp.devices.infrastructure.in.web;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.devices.application.dto.DeviceContextDto;
import es.terencio.erp.devices.application.port.in.GetDeviceContextUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/pos/sync")
@Tag(name = "POS Sync", description = "POS device synchronization endpoints")
public class PosSyncController {

    private final GetDeviceContextUseCase getDeviceContextUseCase;

    public PosSyncController(GetDeviceContextUseCase getDeviceContextUseCase) {
        this.getDeviceContextUseCase = getDeviceContextUseCase;
    }

    // Retained @PreAuthorize("hasRole('DEVICE')") as devices are explicitly assigned this hardcoded machine role by the ApiKeyFilter.
    @GetMapping("/context")
    @PreAuthorize("hasRole('DEVICE')")
    @Operation(summary = "Get device context")
    public ResponseEntity<ApiResponse<DeviceContextDto>> getContext(@AuthenticationPrincipal UUID deviceId) {
        DeviceContextDto context = getDeviceContextUseCase.getContext(deviceId);
        return ResponseEntity.ok(ApiResponse.success("Device context fetched", context));
    }
}
