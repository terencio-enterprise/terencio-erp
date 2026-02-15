package es.terencio.erp.sales.domain.model;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.shared.domain.exception.InvalidStateException;
import es.terencio.erp.shared.domain.exception.InvariantViolationException;
import es.terencio.erp.shared.domain.identifier.DeviceId;
import es.terencio.erp.shared.domain.identifier.StoreId;
import es.terencio.erp.shared.domain.identifier.UserId;
import es.terencio.erp.shared.domain.valueobject.Money;

/**
 * Shift entity.
 * Represents a cash register shift/session.
 */
public class Shift {

    private final UUID id;
    private final StoreId storeId;
    private final DeviceId deviceId;
    private final UserId userId;
    private final Instant openedAt;
    private Instant closedAt;
    private final Money amountInitial;
    private Money amountSystem;
    private Money amountCounted;
    private Money amountDiff;
    private ShiftStatus status;
    private Integer zCount;

    public Shift(
            UUID id,
            StoreId storeId,
            DeviceId deviceId,
            UserId userId,
            Instant openedAt,
            Instant closedAt,
            Money amountInitial,
            Money amountSystem,
            Money amountCounted,
            Money amountDiff,
            ShiftStatus status,
            Integer zCount) {

        if (id == null)
            throw new InvariantViolationException("Shift ID cannot be null");
        if (storeId == null)
            throw new InvariantViolationException("Shift must belong to a store");
        if (deviceId == null)
            throw new InvariantViolationException("Shift must belong to a device");
        if (openedAt == null)
            throw new InvariantViolationException("Opened at cannot be null");

        this.id = id;
        this.storeId = storeId;
        this.deviceId = deviceId;
        this.userId = userId;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
        this.amountInitial = amountInitial != null ? amountInitial : Money.zeroEuros();
        this.amountSystem = amountSystem != null ? amountSystem : Money.zeroEuros();
        this.amountCounted = amountCounted;
        this.amountDiff = amountDiff;
        this.status = status != null ? status : ShiftStatus.OPEN;
        this.zCount = zCount;
    }

    public static Shift open(StoreId storeId, DeviceId deviceId, UserId userId, Money initialAmount) {
        return new Shift(
                UUID.randomUUID(),
                storeId,
                deviceId,
                userId,
                Instant.now(),
                null,
                initialAmount,
                Money.zeroEuros(),
                null,
                null,
                ShiftStatus.OPEN,
                null);
    }

    public void close(Money countedAmount, Money systemCalculated) {
        if (status != ShiftStatus.OPEN) {
            throw new InvalidStateException("Shift is not open");
        }

        this.closedAt = Instant.now();
        this.amountSystem = systemCalculated;
        this.amountCounted = countedAmount;
        this.amountDiff = countedAmount.subtract(systemCalculated);
        this.status = ShiftStatus.CLOSED;
    }

    // Getters
    public UUID id() {
        return id;
    }

    public StoreId storeId() {
        return storeId;
    }

    public DeviceId deviceId() {
        return deviceId;
    }

    public UserId userId() {
        return userId;
    }

    public Instant openedAt() {
        return openedAt;
    }

    public Instant closedAt() {
        return closedAt;
    }

    public Money amountInitial() {
        return amountInitial;
    }

    public Money amountSystem() {
        return amountSystem;
    }

    public Money amountCounted() {
        return amountCounted;
    }

    public Money amountDiff() {
        return amountDiff;
    }

    public ShiftStatus status() {
        return status;
    }

    public Integer zCount() {
        return zCount;
    }
}
