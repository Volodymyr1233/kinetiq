package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.EraseExpiredDataUseCase;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Erases expired personal data on a schedule (ADR 0005). */
@Slf4j
@Component
@RequiredArgsConstructor
class RetentionSweep {

    private final EraseExpiredDataUseCase retention;

    // every 10 minutes keeps "1 hour after an invite ended" true; a failed tick is retried by the next one
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void eraseExpired() {
        try {
            retention.eraseExpired();
        } catch (RuntimeException exception) {
            log.warn("Erasing expired data failed, cause={}", exception.getClass().getSimpleName());
        }
    }
}
