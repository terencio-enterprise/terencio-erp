package es.terencio.erp.shared.domain.identifier;

import java.util.UUID;

/**
 * Typed identifier for Store aggregate.
 */
public final class StoreId extends UuidIdentifier {

    public StoreId(UUID value) {
        super(value);
    }

    public StoreId(String value) {
        super(value);
    }

    public static StoreId create() {
        return new StoreId(generate());
    }
}
