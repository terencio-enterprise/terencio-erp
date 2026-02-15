package es.terencio.erp.shared.domain.identifier;

/**
 * Base class for Long-based identifiers.
 */
public abstract class LongIdentifier extends Identifier<Long> {

    protected LongIdentifier(Long value) {
        super(value);
    }
}
