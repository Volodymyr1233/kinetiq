package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.plan.domain.input.RevisePlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.planchange.domain.input.ApplyPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.EntrySnapshot;
import com.kinetiq.planchange.domain.model.ExpiryCause;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import com.kinetiq.planchange.domain.rule.PlanRuleLimits;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PlanChangeSettlementService implements ApplyPlanChangesUseCase {

    private static final PlanRuleLimits PLAN_RULES = new PlanRuleLimits();

    private final CurrentAccountUseCase accounts;
    private final PlanChangeRepository planChanges;
    private final PlanSource plans;
    private final RevisePlanUseCase revisions;
    private final Clock clock;

    @Override
    public List<PlanChangeId> due() {
        return planChanges.findDue(clock.instant());
    }

    // the lock serialises this with an acceptance in flight; the rule limits of the plan change are not run again: they
    // need the logged weeks of its window, and they passed at creation against the same old values
    @Override
    public void settle(PlanChangeId id) {
        Optional<PlanChange> found = planChanges.findForUpdate(id).filter(PlanChange::isUnsettled);
        if (found.isEmpty()) {
            return;
        }
        PlanChange change = found.get();
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, accounts.find(change.getClient()).getTimeZone());
        if (change.getEffectiveWeek().isAfter(today)) {
            return;
        }
        if (change.isOpen()) {
            expire(change, ExpiryCause.UNDECIDED, now);
            return;
        }
        Optional<PlanSnapshot> plan = plans.currentPlan(change.getClient()).filter(current -> fits(current, change));
        if (plan.isEmpty()) {
            expire(change, ExpiryCause.PLAN_EDITED, now);
            return;
        }
        // a save that raced the check makes the revision fail and this transaction roll back, so the next tick decides again
        revisions.revise(
                change.getClient(),
                plan.get().version(),
                change.getEffectiveWeek(),
                sessions(plan.get().apply(change.getContent())));
        planChanges.update(change.takeEffect(now));
        log.info("Plan change applied, rule={}", change.getRule());
    }

    private void expire(PlanChange change, ExpiryCause cause, Instant now) {
        planChanges.update(change.expire(cause, now));
        log.info("Plan change expired, rule={}, cause={}", change.getRule(), cause);
    }

    // a finished plan has no week for the change to apply to
    private static boolean fits(PlanSnapshot plan, PlanChange change) {
        return !change.getEffectiveWeek().isAfter(plan.lastWeek())
                && plan.stillFits(change.getContent())
                && PLAN_RULES.violations(plan, change.getContent()).isEmpty();
    }

    private static List<Session> sessions(PlanSnapshot plan) {
        return plan.sessions().stream().map(PlanChangeSettlementService::session).toList();
    }

    private static Session session(SessionSnapshot snapshot) {
        return new Session(
                snapshot.name(),
                snapshot.day(),
                snapshot.entries().stream().map(PlanChangeSettlementService::entry).toList());
    }

    private static ExerciseEntry entry(EntrySnapshot entry) {
        return new ExerciseEntry(
                entry.exercise(), entry.sets(), entry.amount(), entry.loadKg(), entry.rpe(), entry.restSeconds());
    }
}
