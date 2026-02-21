package es.terencio.erp.marketing.application.service.campaign;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import es.terencio.erp.marketing.infrastructure.config.MarketingProperties;

public class TrackingLinkService {
    private final MarketingProperties properties;
    private static final Pattern LINK_PATTERN = Pattern.compile("href=\"(https?://[^\"]+)\"");

    public TrackingLinkService(MarketingProperties properties) {
        this.properties = properties;
    }

    public String rewriteLinksForTracking(Long logId, String html) {
        Matcher matcher = LINK_PATTERN.matcher(html);
        StringBuilder sb = new StringBuilder();
        long expiresAt = Instant.now().plus(properties.getLinkExpirationHours(), ChronoUnit.HOURS).toEpochMilli();

        while (matcher.find()) {
            String originalUrl = matcher.group(1);
            if (originalUrl.contains("/marketing/preferences") || originalUrl.contains("/marketing/track/click/")) {
                matcher.appendReplacement(sb, "href=\"" + originalUrl + "\"");
                continue;
            }

            String payload = originalUrl + "|" + expiresAt;
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes());
            String signature = generateHmac(encodedPayload);
            String trackUrl = String.format("%s/api/v1/public/marketing/track/click/%d?p=%s&sig=%s",
                    properties.getPublicBaseUrl(), logId, encodedPayload, signature);

            matcher.appendReplacement(sb, "href=\"" + trackUrl + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public String generateHmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getHmacSecret().getBytes(), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes()));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC generation failed", e);
        }
    }
}