package es.terencio.erp.catalog.application.port.in;

import java.util.List;

import es.terencio.erp.catalog.domain.model.Tax;
import es.terencio.erp.shared.domain.identifier.CompanyId;

/**
 * Input port for listing taxes.
 */
public interface ListTaxesUseCase {

    List<Tax> listTaxes(CompanyId companyId);
}
