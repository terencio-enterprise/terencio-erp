package es.terencio.erp.marketing.domain.model;

import java.util.List;

public class EmailMessage {

    private final String to;
    private final String subject;
    private final String bodyHtml;
    private final List<MarketingAttachment> attachments;
    private final String userToken;

    private EmailMessage(String to, String subject, String bodyHtml,
            List<MarketingAttachment> attachments, String userToken) {
        if (to == null || to.isBlank())
            throw new IllegalArgumentException("EmailMessage 'to' is required");
        if (subject == null || subject.isBlank())
            throw new IllegalArgumentException("EmailMessage 'subject' is required");
        this.to = to;
        this.subject = subject;
        this.bodyHtml = bodyHtml;
        this.attachments = attachments != null ? List.copyOf(attachments) : List.of();
        this.userToken = userToken;
    }

    public static EmailMessage of(String to, String subject, String bodyHtml,
            List<MarketingAttachment> attachments, String userToken) {
        return new EmailMessage(to, subject, bodyHtml, attachments, userToken);
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public List<MarketingAttachment> getAttachments() {
        return attachments;
    }

    public String getUserToken() {
        return userToken;
    }
}
