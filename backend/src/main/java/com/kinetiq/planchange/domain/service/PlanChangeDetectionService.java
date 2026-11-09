package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import com.kinetiq.planchange.domain.rule.PlanChangeRule;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
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
        Optional<ScheduledClient> scheduled = history.scheduledClient(client);
        Instant now = clock.instant();
        // the week is read again here: it may have been checked since due() ran
        if (scheduled.isEmpty()
                || !scheduled.get().isDue(now)
                || !history.claimWeek(client, scheduled.get().checkWeek(now), now)) {
            return;
        }
        List<Finding> findings = evaluate(client, scheduled.get(), scheduled.get().checkWeek(now));
        if (!findings.isEmpty()) {
            log.info("Rules fired, count={}, rules={}", findings.size(), findings.stream().map(Finding::rule).toList());
        }
    }

    // the completed weeks are the ones before the check week; a rule needs its whole window inside the plan
    private List<Finding> evaluate(AccountId client, ScheduledClient scheduled, LocalDate checkWeek) {
        int longest = rules.stream().mapToInt(rule -> rule.rule().weeks()).max().orElse(0);
        LocalDate lastMonday = checkWeek.minusWeeks(1);
        LocalDate lastPlanMonday = scheduled.firstWeek().plusWeeks(scheduled.weeks() - 1L);
        List<PlanChangeRule> eligible = rules.stream()
                .filter(rule -> !windowStart(lastMonday, rule.rule().weeks()).isBefore(scheduled.firstWeek()))
                .filter(rule -> !lastMonday.isAfter(lastPlanMonday))
                .toList();
        if (eligible.isEmpty()) {
            return List.of();
        }
        List<WeekTraining> weeks = history.weeks(client, windowStart(lastMonday, longest), longest);
        List<Finding> findings = new ArrayList<>();
        for (PlanChangeRule rule : eligible) {
            int size = rule.rule().weeks();
            rule.evaluate(weeks.subList(weeks.size() - size, weeks.size())).ifPresent(findings::add);
        }
        findings.sort(Comparator.comparing(Finding::rule));
        return findings;
    }

    private static LocalDate windowStart(LocalDate lastMonday, int weeks) {
        return lastMonday.minusDays((long) (weeks - 1) * DAYS_PER_WEEK);
    }
}
