package es.terencio.erp.shared.domain.identifier;

/**
 * Typed identifier for User aggregate.
 */
public final class UserId extends LongIdentifier {

    public UserId(Long value) {
        super(value);
    }
}
