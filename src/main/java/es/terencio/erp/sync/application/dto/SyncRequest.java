package es.terencio.erp.sync.application.dto;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for the POS polling mechanism.
 */
public record SyncRequest(
        /**
         * The Store ID the POS belongs to.
         * Used to filter store-specific pricing/products/customers.
         */
        @NotNull
        UUID storeId,

        /**
         * The timestamp of the last successful sync on the POS.
         * If null, it implies a full initial load (bootstrap).
         */
        Instant lastSync) {
}
