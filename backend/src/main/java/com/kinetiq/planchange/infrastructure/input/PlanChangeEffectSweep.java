package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.planchange.domain.input.ApplyPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Applies or expires the plan changes whose effective Monday has begun. */
@Slf4j
@Component
@RequiredArgsConstructor
class PlanChangeEffectSweep {

    private final ApplyPlanChangesUseCase settlement;

    // each plan change in its own transaction, so one failure cannot hold up the others; it is retried next tick
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    void settleDue() {
        for (PlanChangeId id : settlement.due()) {
            try {
                settlement.settle(id);
            } catch (RuntimeException exception) {
                log.warn("Plan change settlement failed, cause={}", exception.getClass().getSimpleName());
            }
        }
    }
}
