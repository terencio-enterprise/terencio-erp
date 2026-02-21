package es.terencio.erp.marketing.application.service.campaign;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailRetryPolicy {
    private static final Logger log = LoggerFactory.getLogger(EmailRetryPolicy.class);
    private static final long MAX_BACKOFF_MILLIS = 15_000L;
    private final int maxRetries;

    public EmailRetryPolicy(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public boolean execute(Supplier<Boolean> action, String identifier) {
        int attempts = 0;
        while (attempts <= maxRetries) {
            try {
                if (action.get())
                    return true;
                return false;
            } catch (Exception e) {
                attempts++;
                if (attempts > maxRetries) {
                    log.error("Failed action for {} after {} attempts", identifier, attempts, e);
                    return false;
                }

                log.warn("Failed action for {} on attempt {} of {}. Retrying.", identifier, attempts, maxRetries + 1);

                try {
                    long exponential = (1L << Math.min(attempts, 20)) * 1_000L;
                    long backoff = Math.min(exponential, MAX_BACKOFF_MILLIS);
                    long jitter = ThreadLocalRandom.current().nextLong(250L, 1_001L);
                    Thread.sleep(backoff + jitter);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }
}