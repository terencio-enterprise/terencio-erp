package es.terencio.erp.security.application.port.out;

import es.terencio.erp.security.domain.model.RegistrationCode;
import es.terencio.erp.shared.domain.identifier.StoreId;

import java.util.Optional;

/**
 * Output port for RegistrationCode persistence.
 */
public interface RegistrationCodeRepository {

    RegistrationCode save(RegistrationCode registrationCode);

    Optional<RegistrationCode> findByCode(String code);

    RegistrationCode create(StoreId storeId, String preassignedName, int validityDays);
}
