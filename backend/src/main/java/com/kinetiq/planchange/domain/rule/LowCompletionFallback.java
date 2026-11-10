package com.kinetiq.planchange.domain.rule;

import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.RemovedSession;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Removes the session finished least often over the rule's weeks; of sessions finished equally often, the one on the
 * latest weekday. Nothing moves, and a plan of the smallest size keeps every session.
 */
public final class LowCompletionFallback implements FixedTransformation {

    @Override
    public Rule rule() {
        return Rule.LOW_COMPLETION;
    }

    @Override
    public Optional<PlanChangeContent> apply(PlanSnapshot plan, DuePlanChange due) {
        if (plan.sessions().size() <= Plan.MIN_SESSIONS_WHEN_COMPLETE) {
            return Optional.empty();
        }
        Comparator<SessionSnapshot> leastFinished = Comparator.<SessionSnapshot>comparingLong(
                        session -> finished(due, session.day()))
                .thenComparing(SessionSnapshot::day, Comparator.reverseOrder());
        SessionSnapshot removed = plan.sessions().stream().min(leastFinished).orElseThrow();
        return Optional.of(new PlanChangeContent(
                List.of(), Optional.of(new RemovedSession(removed.day(), removed.name())), List.of()));
    }

    private static long finished(DuePlanChange due, DayOfWeek day) {
        return due.weeks().stream()
                .flatMap(week -> week.finishedDays().stream())
                .filter(finished -> finished == day)
                .count();
    }
}
