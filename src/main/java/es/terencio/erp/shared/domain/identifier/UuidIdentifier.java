package es.terencio.erp.shared.domain.identifier;

import java.util.UUID;

/**
 * Base class for UUID-based identifiers.
 */
public abstract class UuidIdentifier extends Identifier<UUID> {

    protected UuidIdentifier(UUID value) {
        super(value);
    }

    protected UuidIdentifier(String value) {
        super(UUID.fromString(value));
    }

    public static UUID generate() {
        return UUID.randomUUID();
    }
}
