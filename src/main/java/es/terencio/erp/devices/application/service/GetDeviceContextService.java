package es.terencio.erp.devices.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import es.terencio.erp.devices.application.dto.DeviceContextDto;
import es.terencio.erp.devices.application.dto.DeviceDto;
import es.terencio.erp.devices.application.port.in.GetDeviceContextUseCase;
import es.terencio.erp.devices.application.port.out.DevicePort;
import es.terencio.erp.shared.exception.RegistrationException;
import es.terencio.erp.stores.application.dto.StoreDto;
import es.terencio.erp.stores.application.dto.StoreSettingsDto;
import es.terencio.erp.stores.application.port.out.StorePort;
import es.terencio.erp.stores.application.port.out.StoreSettingsPort;
import es.terencio.erp.users.application.dto.UserSyncDto;
import es.terencio.erp.users.application.port.out.UserPort;

/**
 * Service for retrieving device context information.
 * Used by POS terminals during initial sync/handshake.
 */
@Service
public class GetDeviceContextService implements GetDeviceContextUseCase {

    private final DevicePort devicePort;
    private final StorePort storePort;
    private final StoreSettingsPort storeSettingsPort;
    private final UserPort userPort;

    public GetDeviceContextService(
            DevicePort devicePort,
            StorePort storePort,
            StoreSettingsPort storeSettingsPort,
            UserPort userPort) {
        this.devicePort = devicePort;
        this.storePort = storePort;
        this.storeSettingsPort = storeSettingsPort;
        this.userPort = userPort;
    }

    @Override
    public DeviceContextDto getContext(UUID deviceId) {
        // 1. Verify device exists and is ACTIVE
        DeviceDto device = devicePort.findById(deviceId)
                .orElseThrow(() -> new RegistrationException("Device not found"));

        if (!"ACTIVE".equals(device.status())) {
            throw new RegistrationException("Device is not active");
        }

        // 2. Fetch Store information
        StoreDto store = storePort.findById(device.storeId())
                .orElseThrow(() -> new RegistrationException("Store not found"));

        // 3. Fetch Store Settings (may not exist for new stores)
        StoreSettingsDto settings = storeSettingsPort.findByStoreId(device.storeId())
                .orElse(createDefaultSettings(device.storeId()));

        // 4. Fetch all active users for this store (with pinHash for offline
        // verification)
        List<UserSyncDto> users = userPort.findSyncDataByStoreId(device.storeId());

        return new DeviceContextDto(store, settings, users);
    }

    private StoreSettingsDto createDefaultSettings(UUID storeId) {
        return new StoreSettingsDto(
                storeId,
                false, // allowNegativeStock
                null, // defaultTariffId
                true, // printTicketAutomatically
                null // requireCustomerForLargeAmount
        );
    }
}
