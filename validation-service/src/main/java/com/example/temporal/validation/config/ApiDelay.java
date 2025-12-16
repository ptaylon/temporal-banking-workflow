package com.example.temporal.validation.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ApiDelay {

    @Value("${validation.api.delay.ms:15000}")
    private long delayMs;

    public void sleepIfEnabled() {
        if (delayMs <= 0) return;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Applying validation-service API delay of {} ms", delayMs);
            }
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
