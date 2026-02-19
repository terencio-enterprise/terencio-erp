package es.terencio.erp.devices.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import es.terencio.erp.devices.application.dto.DeviceContextDto;
import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.port.in.GetDeviceContextUseCase;
import es.terencio.erp.devices.application.port.out.DevicePort;
import es.terencio.erp.employees.application.dto.EmployeeSyncDto;
import es.terencio.erp.employees.application.port.out.EmployeePort;
import es.terencio.erp.organization.application.port.out.StoreRepository;
import es.terencio.erp.organization.application.port.out.StoreSettingsRepository;
import es.terencio.erp.organization.domain.model.Store;
import es.terencio.erp.organization.domain.model.StoreSettings;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.valueobject.Money;
import es.terencio.erp.shared.exception.RegistrationException;

/**
 * Service for retrieving device context information.
 * Used by POS terminals during initial sync/handshake.
 */
@Service
public class GetDeviceContextService implements GetDeviceContextUseCase {

    private final DevicePort devicePort;
    private final StoreRepository storeRepository;
    private final StoreSettingsRepository storeSettingsRepository;
    private final EmployeePort employeePort;

    public GetDeviceContextService(
            DevicePort devicePort,
            StoreRepository storeRepository,
            StoreSettingsRepository storeSettingsRepository,
            EmployeePort employeePort) {
        this.devicePort = devicePort;
        this.storeRepository = storeRepository;
        this.storeSettingsRepository = storeSettingsRepository;
        this.employeePort = employeePort;
    }

    @Override
    public DeviceContextDto getContext(UUID deviceId) {
        // 1. Verify device exists and is ACTIVE
        DeviceDto device = devicePort.findById(deviceId)
                .orElseThrow(() -> new RegistrationException("Device not found"));

        if (!"ACTIVE".equals(device.status())) {
            throw new RegistrationException("Device is not active");
        }

        StoreId storeId = new StoreId(device.storeId());

        // 2. Fetch Store information
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RegistrationException("Store not found"));

        // 3. Fetch Store Settings (may not exist for new stores)
        StoreSettings settings = storeSettingsRepository.findByStoreId(storeId)
                .orElse(createDefaultSettings(storeId));

        // 4. Fetch all active users for this store (with pinHash for offline
        // verification)
        List<EmployeeSyncDto> users = employeePort.findSyncDataByStoreId(device.storeId());

        return new DeviceContextDto(store, settings, users);
    }

    private StoreSettings createDefaultSettings(StoreId storeId) {
        return new StoreSettings(
                storeId,
                false, // allowNegativeStock
                null, // defaultTariffId
                true, // printTicketAutomatically
                Money.ofEuros(1000.0), // requireCustomerForLargeAmount
                java.time.Instant.now());
    }
}
