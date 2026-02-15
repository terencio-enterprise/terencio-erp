package es.terencio.erp.accounting.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * AccountingEntry aggregate root.
 * Represents a double-entry accounting journal entry.
 */
public class AccountingEntry {

    private final Long id;
    private final CompanyId companyId;
    private final String referenceType;
    private final UUID referenceUuid;
    private final LocalDate entryDate;
    private final String description;
    private final List<AccountingEntryLine> lines;
    private final Instant createdAt;

    public AccountingEntry(
            Long id,
            CompanyId companyId,
            String referenceType,
            UUID referenceUuid,
            LocalDate entryDate,
            String description,
            Instant createdAt) {

        if (companyId == null)
            throw new InvariantViolationException("CompanyId cannot be null");
        if (entryDate == null)
            throw new InvariantViolationException("Entry date cannot be null");

        this.id = id;
        this.companyId = companyId;
        this.referenceType = referenceType;
        this.referenceUuid = referenceUuid;
        this.entryDate = entryDate;
        this.description = description;
        this.lines = new ArrayList<>();
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static AccountingEntry create(
            CompanyId companyId,
            String referenceType,
            UUID referenceUuid,
            LocalDate entryDate,
            String description) {

        return new AccountingEntry(
                null,
                companyId,
                referenceType,
                referenceUuid,
                entryDate,
                description,
                Instant.now());
    }

    public void addLine(AccountingEntryLine line) {
        this.lines.add(line);
    }

    public boolean isBalanced() {
        var totalDebit = lines.stream()
                .map(AccountingEntryLine::debit)
                .reduce(Money::add)
                .orElse(es.terencio.erp.shared.domain.valueobject.Money.zeroEuros());

        var totalCredit = lines.stream()
                .map(AccountingEntryLine::credit)
                .reduce(es.terencio.erp.shared.domain.valueobject.Money::add)
                .orElse(es.terencio.erp.shared.domain.valueobject.Money.zeroEuros());

        return totalDebit.equals(totalCredit);
    }

    public List<AccountingEntryLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    // Getters
    public Long id() {
        return id;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public String referenceType() {
        return referenceType;
    }

    public UUID referenceUuid() {
        return referenceUuid;
    }

    public LocalDate entryDate() {
        return entryDate;
    }

    public String description() {
        return description;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
