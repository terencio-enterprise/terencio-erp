package es.terencio.erp.marketing.infrastructure.adapter.aws;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import es.terencio.erp.marketing.application.port.out.MailingSystemPort;
import es.terencio.erp.marketing.domain.model.EmailMessage;
import es.terencio.erp.marketing.domain.model.MarketingAttachment;
import jakarta.activation.DataSource;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.RawMessage;
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class AwsSesMailingAdapter implements MailingSystemPort {

    private final SesClient sesClient;
    private final S3Client s3Client;

    @Override
    @Async
    public void send(EmailMessage msg) {
        try {
            MimeMessage mimeMessage = createMimeMessage(msg);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            mimeMessage.writeTo(outputStream);

            RawMessage rawMessage = RawMessage.builder()
                    .data(SdkBytes.fromByteArray(outputStream.toByteArray()))
                    .build();

            SendRawEmailRequest request = SendRawEmailRequest.builder()
                    .rawMessage(rawMessage)
                    .build();

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
        mimeMessage.setFrom(new InternetAddress("noreply@terencio.es")); // Configurable
        mimeMessage.setRecipients(Message.RecipientType.TO, InternetAddress.parse(msg.getTo()));

        // Headers for One-Click Unsubscribe
        if (msg.getUserToken() != null) {
            String oneClickUrl = "https://api.terencio.es/v1/public/marketing/unsubscribe-one-click?token="
                    + msg.getUserToken();
            mimeMessage.addHeader("List-Unsubscribe", "<" + oneClickUrl + ">");
            mimeMessage.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
        }

        MimeMultipart multipart = new MimeMultipart("mixed");

        // Body
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setContent(msg.getBodyHtml(), "text/html; charset=UTF-8");
        multipart.addBodyPart(bodyPart);

        // Attachments
        if (msg.getAttachments() != null) {
            for (MarketingAttachment ref : msg.getAttachments()) {
                addAttachmentToMime(multipart, ref);
            }
        }

        mimeMessage.setContent(multipart);
        return mimeMessage;
    }

    private void addAttachmentToMime(MimeMultipart multipart, MarketingAttachment ref)
            throws MessagingException, IOException {
        InputStream s3Stream = s3Client.getObject(b -> b.bucket(ref.getS3Bucket()).key(ref.getS3Key()));

        MimeBodyPart attachmentPart = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(s3Stream, ref.getContentType());
        attachmentPart.setDataHandler(new jakarta.activation.DataHandler(source));
        attachmentPart.setFileName(ref.getFilename());

        multipart.addBodyPart(attachmentPart);
    }
}
