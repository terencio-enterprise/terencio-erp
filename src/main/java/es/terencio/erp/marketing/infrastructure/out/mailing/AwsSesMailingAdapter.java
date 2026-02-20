package es.terencio.erp.marketing.infrastructure.out.mailing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.EmailMessage;

@Component
public class AwsSesMailingAdapter implements MailingSystemPort {
    private static final Logger log = LoggerFactory.getLogger(AwsSesMailingAdapter.class);

    @Override
    public void send(EmailMessage message) {
        log.info("Simulating email to: {} | Subject: {}", message.getTo(), message.getSubject());
    }
}