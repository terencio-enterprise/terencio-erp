package es.terencio.erp.devices.application.port.out;

import java.util.Optional;

import es.terencio.erp.devices.domain.model.RegistrationCode;
import es.terencio.erp.shared.domain.identifier.StoreId;

/**
 * Output port for RegistrationCode persistence.
 */
public interface RegistrationCodeRepository {

    RegistrationCode save(RegistrationCode registrationCode);

    Optional<RegistrationCode> findByCode(String code);

    RegistrationCode create(StoreId storeId, String preassignedName, int validityDays);
}
