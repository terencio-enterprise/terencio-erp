package es.terencio.erp.organization.application.port.in;

import es.terencio.erp.organization.application.usecase.CreateCompanyCommand;
import es.terencio.erp.organization.application.usecase.CreateCompanyResult;

/**
 * Input port for creating companies.
 */
public interface CreateCompanyUseCase {
    CreateCompanyResult execute(CreateCompanyCommand command);
}
