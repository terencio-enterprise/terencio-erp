package es.terencio.erp.shared.domain.identifier;

import java.util.UUID;

/**
 * Typed identifier for Warehouse aggregate.
 */
public final class WarehouseId extends UuidIdentifier {

    public WarehouseId(UUID value) {
        super(value);
    }

    public WarehouseId(String value) {
        super(value);
    }

    public static WarehouseId create() {
        return new WarehouseId(generate());
    }
}
