package es.terencio.erp.sync.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.sync.application.dto.SyncRequest;
import es.terencio.erp.sync.application.dto.SyncResponseDto;
import es.terencio.erp.sync.application.port.in.DownstreamSyncUseCase;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sync")
@CrossOrigin(origins = "*")
public class SyncController {

    private final DownstreamSyncUseCase downstreamSyncUseCase;

    public SyncController(DownstreamSyncUseCase downstreamSyncUseCase) {
        this.downstreamSyncUseCase = downstreamSyncUseCase;
    }

    /**
     * Poll updates for POS.
     * The POS sends { "storeId": "...", "lastSync": "2023-10-27T10:00:00Z" }
     * The Server responds with all data modified after that time.
     */
    @PostMapping("/poll")
    public ResponseEntity<SyncResponseDto> pollUpdates(@Valid @RequestBody SyncRequest request) {
        return ResponseEntity.ok(downstreamSyncUseCase.getUpdates(request));
    }
}
