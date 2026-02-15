package es.terencio.erp.shared.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing a generic percentage (0-100).
 */
public final class Percentage {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal value;

    private Percentage(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Percentage value cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100");
        }
        this.value = value.setScale(SCALE, ROUNDING);
    }

    public static Percentage of(BigDecimal value) {
        return new Percentage(value);
    }

    public static Percentage of(double value) {
        return of(BigDecimal.valueOf(value));
    }

    public static Percentage zero() {
        return of(BigDecimal.ZERO);
    }

    public BigDecimal value() {
        return value;
    }

    public BigDecimal asFactor() {
        return value.divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    public Money applyTo(Money amount) {
        return amount.multiply(asFactor());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Percentage that = (Percentage) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value + "%";
    }
}
