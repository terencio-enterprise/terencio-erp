package es.terencio.erp.shared.domain.valueobject;

import java.util.Objects;

/**
 * Value Object representing a tax identification number (NIF/CIF).
 */
public final class TaxId {

    private final String value;

    private TaxId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TaxId cannot be null or empty");
        }
        this.value = value.trim().toUpperCase();
    }

    public static TaxId of(String value) {
        return new TaxId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TaxId taxId = (TaxId) o;
        return Objects.equals(value, taxId.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
