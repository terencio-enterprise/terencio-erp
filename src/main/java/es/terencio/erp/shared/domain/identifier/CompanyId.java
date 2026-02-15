package es.terencio.erp.shared.domain.identifier;

import java.util.UUID;

/**
 * Typed identifier for Company aggregate.
 */
public final class CompanyId extends UuidIdentifier {

    public CompanyId(UUID value) {
        super(value);
    }

    public CompanyId(String value) {
        super(value);
    }

    public static CompanyId create() {
        return new CompanyId(generate());
    }
}
