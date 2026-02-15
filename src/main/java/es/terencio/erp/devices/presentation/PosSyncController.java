package es.terencio.erp.devices.presentation;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.devices.application.dto.DeviceContextDto;
import es.terencio.erp.devices.application.port.in.GetDeviceContextUseCase;
import es.terencio.erp.shared.presentation.ApiResponse;

/**
 * REST controller for POS device synchronization.
 * Provides endpoints for devices to retrieve their configuration and context.
 */
@RestController
@RequestMapping("/api/v1/pos/sync")
public class PosSyncController {

    private final GetDeviceContextUseCase getDeviceContextUseCase;

    public PosSyncController(GetDeviceContextUseCase getDeviceContextUseCase) {
        this.getDeviceContextUseCase = getDeviceContextUseCase;
    }

    /**
     * Get complete device context for initial sync/handshake.
     * Requires ROLE_DEVICE (authenticated via API Key).
     * 
     * @param deviceId The authenticated device ID (from Principal)
     * @return Device context including store, settings, and users
     */
    @GetMapping("/context")
    @PreAuthorize("hasRole('DEVICE')")
    public ResponseEntity<ApiResponse<DeviceContextDto>> getContext(@AuthenticationPrincipal UUID deviceId) {
        DeviceContextDto context = getDeviceContextUseCase.getContext(deviceId);
        return ResponseEntity.ok(ApiResponse.success("Device context fetched successfully", context));
    }
}
