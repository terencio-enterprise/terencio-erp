package es.terencio.erp.sales.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.terencio.erp.shared.domain.exception.InvalidStateException;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.CompanyId;
import es.terencio.erp.shared.domain.identifier.CustomerId;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.SaleId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.identifier.UserId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * Sale aggregate root.
 * Represents a POS transaction (ticket or invoice).
 */
public class Sale {

    private final Long id;
    private final SaleId uuid;
    private final CompanyId companyId;
    private final StoreId storeId;
    private final DeviceId deviceId;
    private String series;
    private Integer number;
    private String fullReference;
    private DocumentType type;
    private SaleStatus status;
    private UserId userId;
    private CustomerId customerId;
    private final Instant createdAtPos;
    private Instant issuedAtPos;
    private final List<SaleLine> lines;
    private final List<Payment> payments;
    private Money totalNet;
    private Money totalTax;
    private Money totalSurcharge;
    private Money totalAmount;
    private SaleId originalSaleUuid;
    private String refundReason;
    private long version;

    public Sale(
            Long id,
            SaleId uuid,
            CompanyId companyId,
            StoreId storeId,
            DeviceId deviceId,
            String series,
            Integer number,
            String fullReference,
            DocumentType type,
            SaleStatus status,
            UserId userId,
            CustomerId customerId,
            Instant createdAtPos,
            Instant issuedAtPos,
            Money totalNet,
            Money totalTax,
            Money totalSurcharge,
            Money totalAmount,
            SaleId originalSaleUuid,
            String refundReason,
            long version) {

        if (uuid == null)
            throw new InvariantViolationException("Sale UUID cannot be null");
        if (companyId == null)
            throw new InvariantViolationException("Sale must belong to a company");
        if (storeId == null)
            throw new InvariantViolationException("Sale must belong to a store");
        if (deviceId == null)
            throw new InvariantViolationException("Sale must be created from a device");
        if (createdAtPos == null)
            throw new InvariantViolationException("Created at POS cannot be null");

        this.id = id;
        this.uuid = uuid;
        this.companyId = companyId;
        this.storeId = storeId;
        this.deviceId = deviceId;
        this.series = series;
        this.number = number;
        this.fullReference = fullReference;
        this.type = type != null ? type : DocumentType.SIMPLIFIED;
        this.status = status != null ? status : SaleStatus.DRAFT;
        this.userId = userId;
        this.customerId = customerId;
        this.createdAtPos = createdAtPos;
        this.issuedAtPos = issuedAtPos;
        this.lines = new ArrayList<>();
        this.payments = new ArrayList<>();
        this.totalNet = totalNet != null ? totalNet : Money.zeroEuros();
        this.totalTax = totalTax != null ? totalTax : Money.zeroEuros();
        this.totalSurcharge = totalSurcharge != null ? totalSurcharge : Money.zeroEuros();
        this.totalAmount = totalAmount != null ? totalAmount : Money.zeroEuros();
        this.originalSaleUuid = originalSaleUuid;
        this.refundReason = refundReason;
        this.version = version;
    }

    public static Sale createDraft(
            CompanyId companyId,
            StoreId storeId,
            DeviceId deviceId,
            UserId userId,
            DocumentType type) {

        return new Sale(
                null,
                SaleId.create(),
                companyId,
                storeId,
                deviceId,
                null,
                null,
                null,
                type,
                SaleStatus.DRAFT,
                userId,
                null,
                Instant.now(),
                null,
                Money.zeroEuros(),
                Money.zeroEuros(),
                Money.zeroEuros(),
                Money.zeroEuros(),
                null,
                null,
                1);
    }

    public void addLine(SaleLine line) {
        if (status != SaleStatus.DRAFT) {
            throw new InvalidStateException("Cannot add lines to non-draft sale");
        }
        this.lines.add(line);
        recalculateTotals();
    }

    public void issue(String series, int number) {
        if (status != SaleStatus.DRAFT) {
            throw new InvalidStateException("Only draft sales can be issued");
        }
        if (lines.isEmpty()) {
            throw new InvalidStateException("Cannot issue sale without lines");
        }

        this.series = series;
        this.number = number;
        this.fullReference = String.format("%s-%04d", series, number);
        this.issuedAtPos = Instant.now();
        this.status = SaleStatus.ISSUED;
        recalculateTotals();
    }

    public void addPayment(Payment payment) {
        if (status != SaleStatus.ISSUED) {
            throw new InvalidStateException("Cannot add payments to non-issued sale");
        }
        this.payments.add(payment);
    }

    public boolean isFullyPaid() {
        Money totalPaid = payments.stream()
                .map(Payment::amount)
                .reduce(Money.zeroEuros(), Money::add);
        return totalPaid.isGreaterThanOrEqual(totalAmount);
    }

    public void markAsFiscalized() {
        if (status != SaleStatus.ISSUED) {
            throw new InvalidStateException("Only issued sales can be fiscalized");
        }
        this.status = SaleStatus.FISCALIZED;
    }

    private void recalculateTotals() {
        this.totalNet = lines.stream()
                .map(SaleLine::netAmount)
                .reduce(Money.zeroEuros(), Money::add);

        this.totalTax = lines.stream()
                .map(SaleLine::taxAmount)
                .reduce(Money.zeroEuros(), Money::add);

        this.totalAmount = totalNet.add(totalTax).add(totalSurcharge);
    }

    public List<SaleLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public List<Payment> getPayments() {
        return Collections.unmodifiableList(payments);
    }

    // Getters
    public Long id() {
        return id;
    }

    public SaleId uuid() {
        return uuid;
    }

    public CompanyId companyId() {
        return companyId;
    }

    public StoreId storeId() {
        return storeId;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public String series() {
        return series;
    }

    public Integer number() {
        return number;
    }

    public String fullReference() {
        return fullReference;
    }

    public DocumentType type() {
        return type;
    }

    public SaleStatus status() {
        return status;
    }

    public UserId userId() {
        return userId;
    }

    public CustomerId customerId() {
        return customerId;
    }

    public Instant createdAtPos() {
        return createdAtPos;
    }

    public Instant issuedAtPos() {
        return issuedAtPos;
    }

    public Money totalNet() {
        return totalNet;
    }

    public Money totalTax() {
        return totalTax;
    }

    public Money totalSurcharge() {
        return totalSurcharge;
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public SaleId originalSaleUuid() {
        return originalSaleUuid;
    }

    public String refundReason() {
        return refundReason;
    }

    public long version() {
        return version;
    }
}
