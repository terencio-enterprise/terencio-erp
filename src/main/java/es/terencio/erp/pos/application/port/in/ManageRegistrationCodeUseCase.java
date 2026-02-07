package es.terencio.erp.pos.application.port.in;

import es.terencio.erp.pos.application.dto.GenerateCodeRequest;
import es.terencio.erp.pos.application.dto.GeneratedCodeDto;

public interface ManageRegistrationCodeUseCase {

    /**
     * Generates a unique 6-digit code linked to a specific Store and POS Name.
     */
    GeneratedCodeDto generateCode(GenerateCodeRequest request);
}