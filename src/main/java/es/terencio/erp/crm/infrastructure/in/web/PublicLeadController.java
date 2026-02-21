package es.terencio.erp.crm.infrastructure.in.web;

import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import es.terencio.erp.crm.application.port.in.IngestLeadUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/public/companies/{companyId}/leads")
@RequiredArgsConstructor
@Tag(name = "Public Leads", description = "Public endpoint for inbound lead ingestion (Landing pages, Webhooks)")
public class PublicLeadController {

    private final IngestLeadUseCase ingestLeadUseCase;

    @PostMapping
    @Operation(summary = "Ingest an inbound lead")
    public ResponseEntity<Void> ingestLead(
            @PathVariable UUID companyId, 
            @RequestBody IngestLeadUseCase.LeadCommand command) {
        
        ingestLeadUseCase.ingest(companyId, command);
        return ResponseEntity.ok().build();
    }
}