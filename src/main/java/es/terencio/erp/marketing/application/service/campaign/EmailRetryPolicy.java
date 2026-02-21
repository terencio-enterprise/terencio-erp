package es.terencio.erp.marketing.application.service.campaign;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailRetryPolicy {
    private static final Logger log = LoggerFactory.getLogger(EmailRetryPolicy.class);
    private final int maxRetries;

    public EmailRetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean execute(Supplier<Boolean> action, String identifier) {
        int attempts = 0;
        while (attempts <= maxRetries) {
            try {
                if (action.get()) return true;
                return false;
            } catch (Exception e) {
                attempts++;
                log.error("Failed action for {} (attempt {}):", identifier, attempts, e);
                if (attempts > maxRetries) {
                    return false;
                }
                try {
                    Thread.sleep((long) Math.pow(2, attempts) * 1000L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}