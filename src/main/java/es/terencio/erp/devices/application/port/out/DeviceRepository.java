package es.terencio.erp.devices.application.port.out;

import java.util.Optional;

import es.terencio.erp.devices.domain.model.Device;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.StoreId;

/**
 * Output port for Device persistence.
 */
public interface DeviceRepository {

    Device save(Device device);

    Optional<Device> findById(DeviceId id);

    Optional<Device> findByHardwareId(String hardwareId);

    boolean existsByStoreAndSerialCode(StoreId storeId, String serialCode);
}
