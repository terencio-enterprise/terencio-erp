package es.terencio.erp.crm.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/leads")
@RequiredArgsConstructor
public class PublicLeadController {

    private final IngestLeadUseCase ingestLeadUseCase;

    @PostMapping
    public ResponseEntity<Void> ingestLead(@RequestBody IngestLeadUseCase.LeadCommand command) {
        ingestLeadUseCase.ingest(command);
        return ResponseEntity.ok().build();
    }
}
