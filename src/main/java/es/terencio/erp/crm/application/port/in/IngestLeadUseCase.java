package es.terencio.erp.crm.application.port.in;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public interface IngestLeadUseCase {
    void ingest(UUID companyId, LeadCommand command);

    record LeadCommand(
            @NotBlank @Email String email,
            @NotBlank String name,
            String companyName,
            String origin,
            List<String> tags,
            String phone,
            boolean consent) {
    }
}