package es.terencio.erp.marketing.application.service.campaign;

import java.util.Map;

import es.terencio.erp.marketing.application.dto.campaign.CampaignAudienceMember;
import es.terencio.erp.marketing.application.port.out.TemplateEnginePort;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingTemplate;
import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;

public class EmailContentBuilder {
    private final TemplateEnginePort templateEngine;
    private final TrackingLinkService trackingLinkService;
    private final MarketingProperties properties;

    public EmailContentBuilder(TemplateEnginePort templateEngine, TrackingLinkService trackingLinkService, MarketingProperties properties) {
        this.templateEngine = templateEngine;
        this.trackingLinkService = trackingLinkService;
        this.properties = properties;
    }

    public EmailMessage buildMessage(MarketingTemplate tpl, CampaignAudienceMember member, Long logId) {
        String unsubscribeLink = properties.getPublicBaseUrl() + "/api/v1/public/marketing/preferences?token=" + member.unsubscribeToken();
        Map<String, String> vars = Map.of(
                "name", member.name() != null ? member.name() : "Customer",
                "unsubscribe_link", unsubscribeLink
        );

        String body = tpl.compile(vars, templateEngine);
        body = trackingLinkService.rewriteLinksForTracking(logId, body);

        String pixelUrl = properties.getPublicBaseUrl() + "/api/v1/public/marketing/track/open/" + logId + "/pixel.gif";
        body += "<img src=\"" + pixelUrl + "\" width=\"1\" height=\"1\" style=\"display:none;\" />";

        String subject = tpl.compileSubject(vars, templateEngine);

        return EmailMessage.of(member.email(), subject, body, member.unsubscribeToken());
    }
}