package es.terencio.erp.crm.application.port.in;

import java.util.List;

import lombok.Builder;
import lombok.Data;

public interface IngestLeadUseCase {
    void ingest(LeadCommand command);

    @Data
    @Builder
    class LeadCommand {
        private String email;
        private String name;
        private String companyName;
        private String origin; // LANDING, REFERRAL
        private List<String> tags;
        private String phone;
        private boolean consent;
    }
}
