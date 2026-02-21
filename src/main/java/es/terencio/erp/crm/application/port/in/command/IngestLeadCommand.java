package es.terencio.erp.crm.application.port.in.command;

import java.util.List;

public record IngestLeadCommand(
        String email,
        String name,
        String companyName,
        String origin,
        List<String> tags,
        String phone,
        boolean consent
) {}