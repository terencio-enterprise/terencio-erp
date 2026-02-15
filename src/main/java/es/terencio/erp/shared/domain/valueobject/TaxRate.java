package es.terencio.erp.shared.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object representing a tax rate (e.g., 21.0000%).
 */
public final class TaxRate {

    private static final int SCALE = 4;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private final BigDecimal rate;

    private TaxRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("Tax rate cannot be null");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Tax rate must be between 0 and 100");
        }
        this.rate = rate.setScale(SCALE, ROUNDING);
    }

    public static TaxRate of(BigDecimal rate) {
        return new TaxRate(rate);
    }

    public static TaxRate of(double rate) {
        return of(BigDecimal.valueOf(rate));
    }

    public static TaxRate zero() {
        return of(BigDecimal.ZERO);
    }

    public BigDecimal rate() {
        return rate;
    }

    public BigDecimal asFactor() {
        return rate.divide(BigDecimal.valueOf(100), SCALE, ROUNDING);
    }

    public Money calculateTaxAmount(Money baseAmount) {
        return baseAmount.multiply(asFactor());
    }

    public Money calculateGrossAmount(Money netAmount) {
        return netAmount.add(calculateTaxAmount(netAmount));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TaxRate taxRate = (TaxRate) o;
        return Objects.equals(rate, taxRate.rate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rate);
    }

    @Override
    public String toString() {
        return rate + "%";
    }
}
