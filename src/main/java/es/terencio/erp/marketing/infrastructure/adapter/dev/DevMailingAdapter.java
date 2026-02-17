package es.terencio.erp.marketing.infrastructure.adapter.dev;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("!prod")
@Slf4j
public class DevMailingAdapter implements MailingSystemPort {

    @Override
    public void send(EmailMessage msg) {
        log.info("========== [DEV MODE] Sending Email ==========");
        log.info("To: {}", msg.getTo());
        log.info("Subject: {}", msg.getSubject());
        log.info("Body Length: {}", msg.getBodyHtml() != null ? msg.getBodyHtml().length() : 0);
        if (msg.getAttachments() != null && !msg.getAttachments().isEmpty()) {
            log.info("Attachments: {}", msg.getAttachments().size());
        }
        log.info("==============================================");
    }
}
