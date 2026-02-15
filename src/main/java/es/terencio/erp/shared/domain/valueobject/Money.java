package es.terencio.erp.shared.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Value Object representing monetary amounts.
 * Uses cents (long) as the unit of accounting to avoid rounding problems.
 * Immutable.
 */
public final class Money {

    private static final int CENTS_PER_UNIT = 100;

    private final long cents; // Amount in cents (e.g., 1250 = 12.50 EUR)
    private final Currency currency;

    private Money(long cents, Currency currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        this.cents = cents;
        this.currency = currency;
    }

    /**
     * Create Money from cents.
     * 
     * @param cents        Amount in cents (e.g., 1250 for 12.50)
     * @param currencyCode ISO currency code
     */
    public static Money ofCents(long cents, String currencyCode) {
        return new Money(cents, Currency.getInstance(currencyCode));
    }

    /**
     * Create Money from BigDecimal amount (units, not cents).
     * 
     * @param amount       Amount in units (e.g., 12.50)
     * @param currencyCode ISO currency code
     */
    public static Money of(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        long cents = amount.multiply(BigDecimal.valueOf(CENTS_PER_UNIT))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        return new Money(cents, Currency.getInstance(currencyCode));
    }

    public static Money ofEuros(BigDecimal amount) {
        return of(amount, "EUR");
    }

    public static Money ofEuros(double amount) {
        return ofEuros(BigDecimal.valueOf(amount));
    }

    public static Money ofEurosCents(long cents) {
        return ofCents(cents, "EUR");
    }

    public static Money zero(String currencyCode) {
        return ofCents(0, currencyCode);
    }

    public static Money zeroEuros() {
        return zero("EUR");
    }

    /**
     * Get amount in cents (internal representation).
     */
    public long cents() {
        return cents;
    }

    /**
     * Get amount as BigDecimal in units (e.g., 12.50).
     */
    public BigDecimal amount() {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(CENTS_PER_UNIT), 2, RoundingMode.UNNECESSARY);
    }

    public Currency currency() {
        return currency;
    }

    public String currencyCode() {
        return currency.getCurrencyCode();
    }

    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.cents + other.cents, this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        return new Money(this.cents - other.cents, this.currency);
    }

    public Money multiply(BigDecimal factor) {
        if (factor == null) {
            throw new IllegalArgumentException("Factor cannot be null");
        }
        long newCents = BigDecimal.valueOf(this.cents)
                .multiply(factor)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
        return new Money(newCents, this.currency);
    }

    public Money multiply(double factor) {
        return multiply(BigDecimal.valueOf(factor));
    }

    public Money divide(BigDecimal divisor) {
        if (divisor == null) {
            throw new IllegalArgumentException("Divisor cannot be null");
        }
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new ArithmeticException("Division by zero");
        }
        long newCents = BigDecimal.valueOf(this.cents)
                .divide(divisor, 0, RoundingMode.HALF_UP)
                .longValue();
        return new Money(newCents, this.currency);
    }

    public Money negate() {
        return new Money(-this.cents, this.currency);
    }

    public boolean isPositive() {
        return cents > 0;
    }

    public boolean isNegative() {
        return cents < 0;
    }

    public boolean isZero() {
        return cents == 0;
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.cents > other.cents;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.cents >= other.cents;
    }

    public boolean isLessThan(Money other) {
        assertSameCurrency(other);
        return this.cents < other.cents;
    }

    public boolean isLessThanOrEqual(Money other) {
        assertSameCurrency(other);
        return this.cents <= other.cents;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies: " +
                    this.currency + " vs " + other.currency);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Money money = (Money) o;
        return cents == money.cents && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cents, currency);
    }

    @Override
    public String toString() {
        return amount() + " " + currency.getCurrencyCode();
    }
}
