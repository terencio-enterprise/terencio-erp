package es.terencio.erp.shared.domain.identifier;

import java.util.Objects;

/**
 * Base class for all typed identifiers in the domain.
 * Provides type safety and prevents primitive obsession.
 */
public abstract class Identifier<T> {

    private final T value;

    protected Identifier(T value) {
        if (value == null) {
            throw new IllegalArgumentException(getClass().getSimpleName() + " cannot be null");
        }
        this.value = value;
    }

    public T value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Identifier<?> that = (Identifier<?>) o;
        return Objects.equals(value, that.value);
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
