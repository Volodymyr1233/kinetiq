package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.input.RevisePlanUseCase;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanRevision;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.plan.domain.output.PlanRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlanRevisionService implements RevisePlanUseCase {

    private final PlanRepository plans;

    // the lock and the version check make the revision and the new sessions one outcome, whatever a save does meanwhile
    @Override
    public void revise(AccountId client, long expectedVersion, LocalDate effectiveWeek, List<Session> sessions) {
        Plan stored = plans.findByClientForUpdate(client)
                .filter(Plan::isPublished)
                .orElseThrow(PlanNotFoundException::new);
        if (stored.getVersion() != expectedVersion) {
            throw new PlanChangedException();
        }
        Plan revised = stored.revisedTo(sessions);
        plans.saveRevision(new PlanRevision(stored.getId(), effectiveWeek, stored.getSessions()));
        plans.save(revised);
    }
}
