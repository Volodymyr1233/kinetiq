package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.DraftPlanUseCase;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Ends a drafting that is no longer running, such as one a restart cut off, with a failed draft. */
@Slf4j
@Component
@RequiredArgsConstructor
class PlanDraftingSweep {

    private final DraftPlanUseCase drafting;

    // each row in its own transaction, so one bad row cannot hold up the others
    @Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
    void endOverdue() {
        for (AccountId client : drafting.overdue()) {
            try {
                drafting.storeFailed(client);
                log.info("Plan drafting overdue, ended with a failed draft");
            } catch (RuntimeException exception) {
                log.warn("Overdue plan drafting not ended, cause={}", exception.getClass().getSimpleName());
            }
        }
    }
}
