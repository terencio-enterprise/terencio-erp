package es.terencio.erp.devices.application.dto;

import java.util.List;

import es.terencio.erp.employees.application.dto.EmployeeSyncDto;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;

/**
 * Complete context information for a POS device.
 * Used for initial sync/handshake after device registration.
 */
public record DeviceContextDto(
                Store store,
                StoreSettings settings,
                List<EmployeeSyncDto> users) {
}
