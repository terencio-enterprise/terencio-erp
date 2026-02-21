package es.terencio.erp.crm.infrastructure.in.web.dto;

import java.util.List;

import es.terencio.erp.crm.application.port.in.command.IngestLeadCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record IngestLeadRequest(
        @NotBlank @Email String email,
        @NotBlank String name,
        String companyName,
        String origin,
        List<String> tags,
        String phone,
        boolean consent
) {
    public IngestLeadCommand toCommand() {
        return new IngestLeadCommand(email, name, companyName, origin, tags, phone, consent);
    }
}