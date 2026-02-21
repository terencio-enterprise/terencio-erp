package es.terencio.erp.marketing.application.port.out;

import es.terencio.erp.marketing.domain.model.EmailMessage;

public interface MailingSystemPort {
    /**
     * Sends an email and returns the provider's unique Message-ID
     */
    String send(EmailMessage message);
}
