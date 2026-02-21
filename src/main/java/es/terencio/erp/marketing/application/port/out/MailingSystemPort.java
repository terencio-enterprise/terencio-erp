package es.terencio.erp.marketing.application.port.out;

import es.terencio.erp.marketing.domain.model.EmailMessage;

public interface MailingSystemPort {
    String send(EmailMessage message);
}