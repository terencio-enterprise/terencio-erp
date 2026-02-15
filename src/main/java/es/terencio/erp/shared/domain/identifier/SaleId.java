package es.terencio.erp.shared.domain.identifier;

import java.util.UUID;

/**
 * Typed identifier for Sale aggregate (UUID-based for offline-first
 * capability).
 */
public final class SaleId extends UuidIdentifier {

    public SaleId(UUID value) {
        super(value);
    }

    public SaleId(String value) {
        super(value);
    }

    public static SaleId create() {
        return new SaleId(generate());
    }
}
