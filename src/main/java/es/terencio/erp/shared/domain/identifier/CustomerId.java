package es.terencio.erp.shared.domain.identifier;

/**
 * Typed identifier for Customer aggregate.
 */
public final class CustomerId extends LongIdentifier {

    public CustomerId(Long value) {
        super(value);
    }
}
