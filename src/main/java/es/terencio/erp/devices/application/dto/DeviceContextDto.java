package es.terencio.erp.devices.application.dto;

import java.util.List;

import es.terencio.erp.employees.application.dto.EmployeeSyncDto;
import es.terencio.erp.stores.application.dto.StoreDto;
import es.terencio.erp.stores.application.dto.StoreSettingsDto;

/**
 * Complete context information for a POS device.
 * Used for initial sync/handshake after device registration.
 */
public record DeviceContextDto(
        StoreDto store,
        StoreSettingsDto settings,
        List<EmployeeSyncDto> users) {
}
