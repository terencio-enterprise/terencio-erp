package es.terencio.erp.marketing.infrastructure.out.mailing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.EmailMessage;

@Component
@Profile("!prod")
public class DevMailingAdapter implements MailingSystemPort {
    private static final Logger log = LoggerFactory.getLogger(DevMailingAdapter.class);

    @Override
    public void send(EmailMessage msg) {
        log.info("========== [DEV MODE] Sending Email ==========");
        log.info("To: {}", msg.getTo());
        log.info("Subject: {}", msg.getSubject());
        log.info("Body Length: {}", msg.getBodyHtml() != null ? msg.getBodyHtml().length() : 0);
        log.info("==============================================");
    }
}
