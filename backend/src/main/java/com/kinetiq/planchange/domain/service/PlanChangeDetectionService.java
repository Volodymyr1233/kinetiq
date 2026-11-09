package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import com.kinetiq.planchange.domain.rule.PlanChangeRule;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PlanChangeDetectionService implements DetectPlanChangesUseCase {

    private static final int DAYS_PER_WEEK = 7;

    private final TrainingHistory history;
    private final PlanChangeRepository planChanges;
    private final List<PlanChangeRule> rules;
    private final Clock clock;

    @Override
    public List<AccountId> due() {
        Instant now = clock.instant();
        return history.clientsWithPublishedPlan().stream()
                .filter(scheduled -> scheduled.isDue(now))
                .map(ScheduledClient::client)
                .toList();
    }

    @Override
    public void check(AccountId client) {
        Optional<ScheduledClient> found = history.scheduledClient(client);
        Instant now = clock.instant();
        // the week is read again here: it may have been checked since due() ran
        if (found.isEmpty()
                || !found.get().isDue(now)
                || !history.claimWeek(client, found.get().checkWeek(now), now)
                || planChanges.hasOpen(client)) {
            return;
        }
        ScheduledClient scheduled = found.get();
        LocalDate lastMonday = scheduled.checkWeek(now).minusWeeks(1);
        List<Finding> findings = evaluate(client, scheduled, lastMonday);
        if (findings.isEmpty()) {
            return;
        }
        log.info("Rules fired, count={}, rules={}", findings.size(), findings.stream().map(Finding::rule).toList());
        // one plan change per run: the first rule in priority order
        Finding chosen = findings.getFirst();
        planChanges.open(new DuePlanChange(
                client,
                chosen.rule(),
                windowStart(lastMonday, chosen.rule().weeks()),
                lastMonday.plusDays(DAYS_PER_WEEK - 1L),
                chosen.exercises()));
    }

    // the completed weeks are the ones before the check week; a rule needs its whole window inside the plan and after
    // the last plan change, so weeks trained under an older plan never count
    private List<Finding> evaluate(AccountId client, ScheduledClient scheduled, LocalDate lastMonday) {
        LocalDate lastPlanMonday = scheduled.firstWeek().plusWeeks(scheduled.weeks() - 1L);
        if (lastMonday.isAfter(lastPlanMonday)) {
            return List.of();
        }
        LocalDate earliest = earliestWeek(client, scheduled);
        List<PlanChangeRule> eligible = rules.stream()
                .filter(rule -> !windowStart(lastMonday, rule.rule().weeks()).isBefore(earliest))
                .sorted(Comparator.comparing(PlanChangeRule::rule))
                .toList();
        if (eligible.isEmpty()) {
            return List.of();
        }
        int longest = eligible.stream().mapToInt(rule -> rule.rule().weeks()).max().orElseThrow();
        List<WeekTraining> weeks = history.weeks(client, windowStart(lastMonday, longest), longest);
        return eligible.stream()
                .flatMap(rule -> {
                    int size = rule.rule().weeks();
                    return rule.evaluate(weeks.subList(weeks.size() - size, weeks.size())).stream();
                })
                .toList();
    }

    // the first Monday that begins after the last plan change ended, or the first plan week if that is later
    private LocalDate earliestWeek(AccountId client, ScheduledClient scheduled) {
        return planChanges
                .lastEndedAt(client)
                .map(ended -> {
                    LocalDate monday = ended.atZone(scheduled.zone())
                            .toLocalDate()
                            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
                    // a Monday that began before the end already had training under the old plan
                    return monday.atStartOfDay(scheduled.zone()).toInstant().isBefore(ended)
                            ? monday.plusWeeks(1)
                            : monday;
                })
                .filter(monday -> monday.isAfter(scheduled.firstWeek()))
                .orElse(scheduled.firstWeek());
    }

    private static LocalDate windowStart(LocalDate lastMonday, int weeks) {
        return lastMonday.minusDays((long) (weeks - 1) * DAYS_PER_WEEK);
    }
}
