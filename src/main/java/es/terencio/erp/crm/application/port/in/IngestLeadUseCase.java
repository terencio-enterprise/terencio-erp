package es.terencio.erp.crm.application.port.in;

import java.util.UUID;

import es.terencio.erp.crm.application.port.in.command.IngestLeadCommand;

public interface IngestLeadUseCase {
    void ingest(UUID companyId, IngestLeadCommand command);
}