package es.terencio.erp.marketing.infrastructure.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "terencio.marketing")
@Data
public class MarketingProperties {
    
    @NotBlank(message = "Public Base URL is required for tracking links")
    private String publicBaseUrl;
    
    @NotBlank(message = "HMAC Secret is required to prevent Open Redirect attacks")
    private String hmacSecret;
    
    private int batchSize = 500;
    private int maxRetries = 3;
    private double rateLimitPerSecond = 14.0; 
    private long linkExpirationHours = 168; 
    private List<String> allowedRedirectDomains = List.of(); 
}