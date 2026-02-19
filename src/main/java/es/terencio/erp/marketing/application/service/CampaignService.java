package es.terencio.erp.marketing.application.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.terencio.erp.marketing.application.dto.CampaignRequest;
import es.terencio.erp.marketing.application.dto.CampaignResult;
import es.terencio.erp.marketing.application.port.in.LaunchCampaignUseCase;
import es.terencio.erp.marketing.application.port.out.CampaignRepositoryPort;
import es.terencio.erp.marketing.application.port.out.CustomerIntegrationPort;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.CampaignLog;
import es.terencio.erp.marketing.domain.model.DeliveryStatus;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.shared.exception.ResourceNotFoundException;

@Service
public class CampaignService implements LaunchCampaignUseCase {

    private static final Logger log = LoggerFactory.getLogger(CampaignService.class);

    private final CampaignRepositoryPort campaignRepository;
    private final CustomerIntegrationPort customerPort;
    private final MailingSystemPort mailingSystem;

    public CampaignService(CampaignRepositoryPort campaignRepository, CustomerIntegrationPort customerPort,
            MailingSystemPort mailingSystem) {
        this.campaignRepository = campaignRepository;
        this.customerPort = customerPort;
        this.mailingSystem = mailingSystem;
    }

    @Override
    @Transactional
    public CampaignResult launch(Long templateId, CampaignRequest.AudienceFilter filter) {
        MarketingTemplate tpl = campaignRepository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));

        List<CustomerIntegrationPort.MarketingCustomer> audience = customerPort.findAudience(filter);

        int sentCount = 0;

        for (CustomerIntegrationPort.MarketingCustomer customer : audience) {
            if (!customer.isCanReceiveMarketing()) {
                continue;
            }

            try {
                sendEmailToCustomer(tpl, customer);
                sentCount++;
            } catch (Exception e) {
                log.error("Failed to send campaign email to {}", customer.getEmail(), e);
                saveLog(tpl, customer, DeliveryStatus.FAILED, e.getMessage());
            }
        }

        return new CampaignResult(sentCount);
    }

    private void sendEmailToCustomer(MarketingTemplate tpl, CustomerIntegrationPort.MarketingCustomer customer) {
        String personalBody = tpl.compile(Map.of("name", customer.getName(), "unsubscribe_link",
                generateUnsubscribeLink(customer.getUnsubscribeToken())));
        String personalSubject = tpl.compileSubject(Map.of("name", customer.getName()));

        EmailMessage msg = EmailMessage.of(customer.getEmail(), personalSubject, personalBody,
                customer.getUnsubscribeToken());
        mailingSystem.send(msg);
        saveLog(tpl, customer, DeliveryStatus.SENT, null);
    }

    private void saveLog(MarketingTemplate tpl, CustomerIntegrationPort.MarketingCustomer customer,
            DeliveryStatus status, String error) {
        CampaignLog logEntry = new CampaignLog(null, customer.getCompanyId(), customer.getId(), tpl.getId(),
                Instant.now(), status, null, error);
        campaignRepository.saveLog(logEntry);
    }

    private String generateUnsubscribeLink(String token) {
        return "https://api.terencio.es/v1/public/marketing/preferences?token=" + token;
    }

    @Override
    public void dryRun(Long templateId, String testEmail) {
        MarketingTemplate tpl = campaignRepository.findTemplateById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
        String personalBody = tpl.compile(Map.of("name", "Test User", "unsubscribe_link", "#"));
        EmailMessage msg = EmailMessage.of(testEmail, "[DRY RUN] " + tpl.getSubjectTemplate(), personalBody,
                "dry-run-token");
        mailingSystem.send(msg);
    }
}