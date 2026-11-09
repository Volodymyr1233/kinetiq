package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * Stands in until plan changes are stored (task #132, which replaces this class): no plan change is ever open or
 * ended, so a due one is only logged, by rule name.
 */
@Slf4j
@Repository
class LoggingPlanChangeRepository implements PlanChangeRepository {

    @Override
    public boolean hasOpen(AccountId client) {
        return false;
    }

    @Override
    public Optional<Instant> lastEndedAt(AccountId client) {
        return Optional.empty();
    }

    @Override
    public void open(DuePlanChange due) {
        log.info("Plan change due, rule={}", due.rule());
    }
}
