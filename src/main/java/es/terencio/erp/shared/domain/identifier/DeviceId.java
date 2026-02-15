package es.terencio.erp.shared.domain.identifier;

import java.util.UUID;

/**
 * Typed identifier for Device aggregate.
 */
public final class DeviceId extends UuidIdentifier {

    public DeviceId(UUID value) {
        super(value);
    }

    public DeviceId(String value) {
        super(value);
    }

    public static DeviceId create() {
        return new DeviceId(generate());
    }
}
