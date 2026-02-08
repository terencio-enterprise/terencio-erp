package es.terencio.erp.sync.application.port.out;

import java.time.Instant;
import java.util.UUID;

import es.terencio.erp.sync.application.dto.SyncResponseDto;

public interface LoadSyncDataPort {

    /**
     * Infrastructure port to query all tables for changes after 'fromTime'.
     *
     * @param storeId The context of the POS (to fetch store-specific overrides).
     * @param fromTime The timestamp of the last sync (can be epoch 0 for full load).
     * @return The populated Sync Response.
     */
    SyncResponseDto fetchChanges(UUID storeId, Instant fromTime);
}
