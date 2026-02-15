package es.terencio.erp.devices.application.port.in;

import java.util.UUID;

import es.terencio.erp.devices.application.dto.DeviceContextDto;

/**
 * Use case for retrieving device context information.
 * Used by POS terminals during initial sync/handshake.
 */
public interface GetDeviceContextUseCase {
    /**
     * Get complete context for a device including store, settings, and users.
     * 
     * @param deviceId The UUID of the authenticated device
     * @return DeviceContextDto containing all necessary information
     */
    DeviceContextDto getContext(UUID deviceId);
}
