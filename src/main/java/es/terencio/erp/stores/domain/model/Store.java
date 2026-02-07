package es.terencio.erp.stores.domain.model;

import java.util.UUID;

/**
 * Domain model representing a store/location.
 * This is owned by the stores module.
 */
public record Store(
        UUID id,
        String code,
        String name,
        String address,
        String taxId,
        boolean isActive) {
}
