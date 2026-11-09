package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Checks every client whose Monday 03:00 has passed, once a week. */
@Slf4j
@Component
@RequiredArgsConstructor
class PlanChangeSweep {

    private final DetectPlanChangesUseCase detection;

    // each client in its own transaction, so one bad client cannot hold up the others; a failed one is retried next tick
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    void checkDue() {
        for (AccountId client : detection.due()) {
            try {
                detection.check(client);
            } catch (RuntimeException exception) {
                log.warn("Plan change check failed, cause={}", exception.getClass().getSimpleName());
            }
        }
    }
}
