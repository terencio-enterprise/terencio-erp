package es.terencio.erp.sync.application.port.in;

import es.terencio.erp.sync.application.dto.SyncRequest;
import es.terencio.erp.sync.application.dto.SyncResponseDto;

public interface DownstreamSyncUseCase {

    /**
     * Retrieves all master data changes (delta) since the last sync.
     * Includes logic to merge Global Data + Store Specific Data.
     */
    SyncResponseDto getUpdates(SyncRequest request);
}
