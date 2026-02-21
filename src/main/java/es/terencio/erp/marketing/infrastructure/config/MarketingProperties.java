package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "app.marketing")
@Data
public class MarketingProperties {
    
    @NotBlank(message = "Public Base URL is required for tracking links")
    private String publicBaseUrl;
    
    @NotBlank(message = "HMAC Secret is required to prevent Open Redirect attacks")
    private String hmacSecret;
    
    private int batchSize = 500;
    private int maxRetries = 3;
    private int rateLimitPerSecond = 14; 
    private long linkExpirationHours = 168; 
}