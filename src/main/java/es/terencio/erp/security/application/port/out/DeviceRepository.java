package es.terencio.erp.security.application.port.out;

import es.terencio.erp.security.domain.model.Device;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.StoreId;

import java.util.Optional;

/**
 * Output port for Device persistence.
 */
public interface DeviceRepository {

    Device save(Device device);

    Optional<Device> findById(DeviceId id);

    Optional<Device> findByHardwareId(String hardwareId);

    boolean existsByStoreAndSerialCode(StoreId storeId, String serialCode);
}
