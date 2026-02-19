package es.terencio.erp.marketing.infrastructure.out.mailing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

@Component
@Profile("prod")
public class AwsSesMailingAdapter implements MailingSystemPort {
    private static final Logger log = LoggerFactory.getLogger(AwsSesMailingAdapter.class);
    private final SesClient sesClient;

    public AwsSesMailingAdapter(SesClient sesClient) {
        this.sesClient = sesClient;
    }

    @Override
    @Async
    public void send(EmailMessage msg) {
        try {
            MimeMessage mimeMessage = createMimeMessage(msg);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);

            RawMessage rawMessage = RawMessage.builder().data(SdkBytes.fromByteArray(outputStream.toByteArray())).build();
            SendRawEmailRequest request = SendRawEmailRequest.builder().rawMessage(rawMessage).build();

            sesClient.sendRawEmail(request);
            log.info("Email sent to {}", msg.getTo());
        } catch (Exception e) {
            log.error("Failed to send email to {}", msg.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private MimeMessage createMimeMessage(EmailMessage msg) throws MessagingException, IOException {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage mimeMessage = new MimeMessage(session);

        mimeMessage.setSubject(msg.getSubject(), "UTF-8");
        mimeMessage.setFrom(new InternetAddress("noreply@terencio.es"));
        mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(msg.getTo()));

        if (msg.getUserToken() != null) {
            String oneClickUrl = "https://api.terencio.es/v1/public/marketing/unsubscribe-one-click?token=" + msg.getUserToken();
            mimeMessage.addHeader("List-Unsubscribe", "<" + oneClickUrl + ">");
            mimeMessage.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
        }

        MimeMultipart multipart = new MimeMultipart("mixed");
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(msg.getBodyHtml(), "text/html; charset=UTF-8");
        multipart.addBodyPart(bodyPart);

        mimeMessage.setContent(multipart);
        return mimeMessage;
    }
}
