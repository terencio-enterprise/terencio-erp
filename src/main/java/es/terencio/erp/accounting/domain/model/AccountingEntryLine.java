package es.terencio.erp.accounting.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * AccountingEntryLine entity.
 * Represents a single debit or credit line in an accounting entry.
 */
public class AccountingEntryLine {

    private final Long id;
    private final Long entryId;
    private final String accountCode;
    private final Money debit;
    private final Money credit;

    public AccountingEntryLine(
            Long id,
            Long entryId,
            String accountCode,
            Money debit,
            Money credit) {

        if (accountCode == null || accountCode.isBlank())
            throw new InvariantViolationException("Account code cannot be empty");
        if (debit == null)
            debit = Money.zeroEuros();
        if (credit == null)
            credit = Money.zeroEuros();

        // Invariant: Only one side can have value
        if (debit.isPositive() && credit.isPositive()) {
            throw new InvariantViolationException("Entry line cannot have both debit and credit");
        }
        if (!debit.isPositive() && !credit.isPositive()) {
            throw new InvariantViolationException("Entry line must have either debit or credit");
        }

        this.id = id;
        this.entryId = entryId;
        this.accountCode = accountCode;
        this.debit = debit;
        this.credit = credit;
    }

    public static AccountingEntryLine debit(String accountCode, Money amount) {
        return new AccountingEntryLine(null, null, accountCode, amount, Money.zeroEuros());
    }

    public static AccountingEntryLine credit(String accountCode, Money amount) {
        return new AccountingEntryLine(null, null, accountCode, Money.zeroEuros(), amount);
    }

    // Getters
    public Long id() {
        return id;
    }

    public Long entryId() {
        return entryId;
    }

    public String accountCode() {
        return accountCode;
    }

    public Money debit() {
        return debit;
    }

    public Money credit() {
        return credit;
    }
}
