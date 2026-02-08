package es.terencio.erp.sync.application.service;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.sync.application.dto.SyncRequest;
import es.terencio.erp.sync.application.dto.SyncResponseDto;
import es.terencio.erp.sync.application.port.in.DownstreamSyncUseCase;
import es.terencio.erp.sync.application.port.out.LoadSyncDataPort;

@Service
public class DownstreamSyncService implements DownstreamSyncUseCase {

    private final LoadSyncDataPort loadSyncDataPort;

    public DownstreamSyncService(LoadSyncDataPort loadSyncDataPort) {
        this.loadSyncDataPort = loadSyncDataPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SyncResponseDto getUpdates(SyncRequest request) {
        // If it's a fresh install (null timestamp), we sync from the beginning of time (Epoch)
        Instant fromTime = request.lastSync() != null ? request.lastSync() : Instant.EPOCH;

        // Delegate to the infrastructure adapter to fetch data
        return loadSyncDataPort.fetchChanges(request.storeId(), fromTime);
    }
}
