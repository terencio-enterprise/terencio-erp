package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.marketing")
@Data
public class MarketingProperties {
    private String publicBaseUrl = "https://api.terencio.es";
    private String hmacSecret = "ChangeThisSecretInProductionEnvironment123456789!";
    private int batchSize = 500;
    private int maxRetries = 3;
    private int rateLimitPerSecond = 14; // AWS SES Default is 14/sec
    private boolean enforceSnsSignature = false; // Toggle for dev/prod
}
