package es.terencio.erp.crm.infrastructure.in.web;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/public/companies/{companyId}/leads")
@Tag(name = "Public Leads", description = "Public endpoint for inbound lead ingestion")
public class PublicLeadController {

    private final IngestLeadUseCase ingestLeadUseCase;

    public PublicLeadController(IngestLeadUseCase ingestLeadUseCase) {
        this.ingestLeadUseCase = ingestLeadUseCase;
    }

    @PostMapping
    @Operation(summary = "Ingest lead")
    public ResponseEntity<Void> ingestLead(@PathVariable UUID companyId, @RequestBody IngestLeadUseCase.LeadCommand command) {
        ingestLeadUseCase.ingest(companyId, command);
        return ResponseEntity.ok().build();
    }
}
