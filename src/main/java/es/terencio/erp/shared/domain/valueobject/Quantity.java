package es.terencio.erp.shared.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing product quantities.
 * Supports fractional quantities (e.g., 1.5 kg).
 */
public final class Quantity {

    private static final int SCALE = 3;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal value;

    private Quantity(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Quantity value cannot be null");
        }
        this.value = value.setScale(SCALE, ROUNDING);
    }

    public static Quantity of(BigDecimal value) {
        return new Quantity(value);
    }

    public static Quantity of(double value) {
        return of(BigDecimal.valueOf(value));
    }

    public static Quantity of(int value) {
        return of(BigDecimal.valueOf(value));
    }

    public static Quantity zero() {
        return of(BigDecimal.ZERO);
    }

    public static Quantity one() {
        return of(BigDecimal.ONE);
    }

    public BigDecimal value() {
        return value;
    }

    public Quantity add(Quantity other) {
        return new Quantity(this.value.add(other.value));
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(this.value.subtract(other.value));
    }

    public Quantity multiply(BigDecimal factor) {
        return new Quantity(this.value.multiply(factor));
    }

    public Quantity negate() {
        return new Quantity(this.value.negate());
    }

    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return value.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }

    public boolean isGreaterThan(Quantity other) {
        return this.value.compareTo(other.value) > 0;
    }

    public boolean isLessThan(Quantity other) {
        return this.value.compareTo(other.value) < 0;
    }

    public boolean isGreaterThanOrEqual(Quantity other) {
        return this.value.compareTo(other.value) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Quantity quantity = (Quantity) o;
        return Objects.equals(value, quantity.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
