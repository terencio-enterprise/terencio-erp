package es.terencio.erp.marketing.domain.model;

import es.terencio.erp.shared.domain.exception.InvariantViolationException;

public class EmailMessage {
    private final String to;
    private final String subject;
    private final String bodyHtml;
    private final String userToken;

    private EmailMessage(String to, String subject, String bodyHtml, String userToken) {
        if (to == null || to.isBlank()) throw new InvariantViolationException("EmailMessage 'to' is required");
        if (subject == null || subject.isBlank()) throw new InvariantViolationException("EmailMessage 'subject' is required");
        
        this.to = to;
        this.subject = subject;
        this.bodyHtml = bodyHtml;
        this.userToken = userToken;
    }

    public static EmailMessage of(String to, String subject, String bodyHtml, String userToken) {
        return new EmailMessage(to, subject, bodyHtml, userToken);
    }

    public String getTo() { return to; }
    public String getSubject() { return subject; }
    public String getBodyHtml() { return bodyHtml; }
    public String getUserToken() { return userToken; }
}