package es.terencio.erp.fiscal.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.SaleId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * FiscalAuditLog entity (IMMUTABLE).
 * Represents the cryptographic chain for VeriFactu compliance.
 */
public class FiscalAuditLog {

    private final Long id;
    private final UUID uuid;
    private final SaleId saleUuid;
    private final StoreId storeId;
    private final DeviceId deviceId;
    private final String eventType;
    private final String previousRecordHash;
    private final int chainSequenceId;
    private final String recordHash;
    private final String signature;
    private final String softwareId;
    private final String softwareVersion;
    private final String developerId;
    private final Money invoiceAmount;
    private final Instant invoiceDate;
    private final String aeatStatus;
    private final Instant createdAt;

    public FiscalAuditLog(
            Long id,
            UUID uuid,
            SaleId saleUuid,
            StoreId storeId,
            DeviceId deviceId,
            String eventType,
            String previousRecordHash,
            int chainSequenceId,
            String recordHash,
            String signature,
            String softwareId,
            String softwareVersion,
            String developerId,
            Money invoiceAmount,
            Instant invoiceDate,
            String aeatStatus,
            Instant createdAt) {

        if (uuid == null)
            throw new InvariantViolationException("Fiscal log UUID cannot be null");
        if (saleUuid == null)
            throw new InvariantViolationException("Sale UUID cannot be null");
        if (deviceId == null)
            throw new InvariantViolationException("Device ID cannot be null");
        if (previousRecordHash == null || previousRecordHash.length() != 64)
            throw new InvariantViolationException("Previous hash must be 64 characters");
        if (recordHash == null || recordHash.length() != 64)
            throw new InvariantViolationException("Record hash must be 64 characters");
        if (chainSequenceId < 1)
            throw new InvariantViolationException("Chain sequence must be >= 1");

        this.id = id;
        this.uuid = uuid;
        this.saleUuid = saleUuid;
        this.storeId = storeId;
        this.deviceId = deviceId;
        this.eventType = eventType != null ? eventType : "ISSUE";
        this.previousRecordHash = previousRecordHash;
        this.chainSequenceId = chainSequenceId;
        this.recordHash = recordHash;
        this.signature = signature;
        this.softwareId = softwareId;
        this.softwareVersion = softwareVersion;
        this.developerId = developerId;
        this.invoiceAmount = invoiceAmount;
        this.invoiceDate = invoiceDate;
        this.aeatStatus = aeatStatus != null ? aeatStatus : "PENDING";
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static FiscalAuditLog create(
            SaleId saleUuid,
            StoreId storeId,
            DeviceId deviceId,
            String previousHash,
            int chainSequence,
            String recordHash,
            String softwareId,
            String softwareVersion,
            String developerId,
            Money invoiceAmount,
            Instant invoiceDate) {

        return new FiscalAuditLog(
                null,
                UUID.randomUUID(),
                saleUuid,
                storeId,
                deviceId,
                "ISSUE",
                previousHash,
                chainSequence,
                recordHash,
                null,
                softwareId,
                softwareVersion,
                developerId,
                invoiceAmount,
                invoiceDate,
                "PENDING",
                Instant.now());
    }

    // Getters
    public Long id() {
        return id;
    }

    public UUID uuid() {
        return uuid;
    }

    public SaleId saleUuid() {
        return saleUuid;
    }

    public StoreId storeId() {
        return storeId;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public String eventType() {
        return eventType;
    }

    public String previousRecordHash() {
        return previousRecordHash;
    }

    public int chainSequenceId() {
        return chainSequenceId;
    }

    public String recordHash() {
        return recordHash;
    }

    public String signature() {
        return signature;
    }

    public String softwareId() {
        return softwareId;
    }

    public String softwareVersion() {
        return softwareVersion;
    }

    public String developerId() {
        return developerId;
    }

    public Money invoiceAmount() {
        return invoiceAmount;
    }

    public Instant invoiceDate() {
        return invoiceDate;
    }

    public String aeatStatus() {
        return aeatStatus;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
