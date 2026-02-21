package es.terencio.erp.marketing.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Enables background execution for campaigns so HTTP requests don't timeout
}
