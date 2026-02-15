package es.terencio.erp.accounting.application.port.out;

import es.terencio.erp.accounting.domain.model.AccountingEntry;

/**
 * Output port for AccountingEntry persistence.
 */
public interface AccountingEntryRepository {

    AccountingEntry save(AccountingEntry entry);
}
