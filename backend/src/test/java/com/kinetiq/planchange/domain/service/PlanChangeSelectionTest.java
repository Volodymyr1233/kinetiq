package com.kinetiq.planchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.LoggedSet;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.rule.HighRpeRule;
import com.kinetiq.planchange.domain.rule.PlanChangeRule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Which plan change is due, with the rules stubbed to fire. The client has a plan of 10 weeks from 2026-09-07 and is
 * checked on Monday 2026-11-09 for the weeks before it, so the last completed week begins on 2026-11-02.
 */
class PlanChangeSelectionTest {

    private static final String CHECK_TIME = "2026-11-09T05:00:00Z";
    private static final ExerciseId SQUAT = ExerciseId.random();

    private final FakeTrainingHistory history = new FakeTrainingHistory();
    private final FakePlanChangeRepository planChanges = new FakePlanChangeRepository();
    private final MutableClock clock = new MutableClock(CHECK_TIME);
    private final AccountId client = history.publishedPlan("Europe/Warsaw");
    private final List<DuePlanChange> chosen = new ArrayList<>();

    /** Fires every time it is asked. */
    private record FiringRule(Rule rule, Set<ExerciseId> exercises) implements PlanChangeRule {

        @Override
        public Optional<Finding> evaluate(List<WeekTraining> window) {
            return Optional.of(new Finding(rule, exercises, List.of()));
        }
    }

    private void check(Rule... fired) {
        List<PlanChangeRule> rules = Arrays.stream(fired)
                .map(rule -> (PlanChangeRule) new FiringRule(rule, Set.of()))
                .toList();
        run(rules);
    }

    private void run(List<PlanChangeRule> rules) {
        new PlanChangeDetectionService(
                        history, planChanges, rules, new FakePlanSource(), new FakePlanChangeDrafter().drafting(), clock)
                .chooseDue(client).ifPresent(chosen::add);
    }

    private List<Rule> dueRules() {
        return chosen.stream().map(DuePlanChange::rule).toList();
    }

    @Test
    void shouldReportLowCompletion_whenAllThreeRulesFire() {
        check(Rule.STALLED_WEIGHT, Rule.HIGH_RPE, Rule.LOW_COMPLETION);

        assertThat(dueRules()).containsExactly(Rule.LOW_COMPLETION);
    }

    @Test
    void shouldReportHighRpe_whenRpeAndStalledWeightFire() {
        check(Rule.STALLED_WEIGHT, Rule.HIGH_RPE);

        assertThat(dueRules()).containsExactly(Rule.HIGH_RPE);
    }

    @Test
    void shouldReportStalledWeight_whenOnlyItFires() {
        check(Rule.STALLED_WEIGHT);

        assertThat(dueRules()).containsExactly(Rule.STALLED_WEIGHT);
    }

    @Test
    void shouldReportNothing_whenNoRuleFires() {
        check();

        assertThat(chosen).isEmpty();
    }

    @Test
    void shouldCarryTheRuleTheWindowAndTheExercises() {
        PlanChangeRule fires = new FiringRule(Rule.STALLED_WEIGHT, Set.of(SQUAT));

        run(List.of(fires));

        assertThat(chosen).hasSize(1);
        DuePlanChange due = chosen.getFirst();
        assertThat(due.client()).isEqualTo(client);
        assertThat(due.zone()).isEqualTo(ZoneId.of("Europe/Warsaw"));
        assertThat(due.rule()).isEqualTo(Rule.STALLED_WEIGHT);
        assertThat(due.windowStart()).isEqualTo(LocalDate.parse("2026-10-19"));
        assertThat(due.windowEnd()).isEqualTo(LocalDate.parse("2026-11-08"));
        assertThat(due.exercises()).containsExactly(SQUAT);
        assertThat(due.weeks())
                .extracting(WeekTraining::monday)
                .containsExactly(LocalDate.parse("2026-10-19"), LocalDate.parse("2026-10-26"), LocalDate.parse("2026-11-02"));
    }

    @Test
    void shouldReportTheExercisesOfARealRuleOverLoggedWeeks() {
        history.trained(new WeekTraining(
                LocalDate.parse("2026-10-26"), 3, 3, 3, List.of(new LoggedSet(SQUAT, null, 9), new LoggedSet(SQUAT, null, 9)), List.of()));
        history.trained(new WeekTraining(LocalDate.parse("2026-11-02"), 3, 3, 3, List.of(new LoggedSet(SQUAT, null, 10)), List.of()));

        run(List.of(new HighRpeRule()));

        assertThat(chosen).hasSize(1);
        assertThat(chosen.getFirst().rule()).isEqualTo(Rule.HIGH_RPE);
        assertThat(chosen.getFirst().exercises()).containsExactly(SQUAT);
        assertThat(chosen.getFirst().windowStart()).isEqualTo(LocalDate.parse("2026-10-26"));
        assertThat(chosen.getFirst().figures()).hasSize(2);
    }

    @Test
    void shouldReportNothingAndReadNoHistory_whenThePlanChangeIsOpen() {
        planChanges.open = true;

        check(Rule.LOW_COMPLETION, Rule.HIGH_RPE, Rule.STALLED_WEIGHT);

        assertThat(chosen).isEmpty();
        assertThat(history.weekReads).isZero();
    }

    @Test
    void shouldClaimTheWeek_evenWhenNothingFiresOrAPlanChangeIsOpen() {
        planChanges.open = true;
        check(Rule.LOW_COMPLETION);

        assertThat(history.claimedWeeks(client)).containsExactly(LocalDate.parse("2026-11-09"));
    }

    @Test
    void shouldReportOnlyOnePlanChange_whenTheRunIsRepeated() {
        check(Rule.LOW_COMPLETION, Rule.HIGH_RPE);
        check(Rule.LOW_COMPLETION, Rule.HIGH_RPE);

        assertThat(chosen).hasSize(1);
    }

    @Test
    void shouldBlockEveryRule_whenThePlanChangeEndedOneWeekAgo() {
        // it ended on the Monday that began the last completed week: its window cannot hold two full weeks since
        planChanges.endedAt = at("2026-11-01T23:00:00Z");

        check(Rule.LOW_COMPLETION, Rule.HIGH_RPE, Rule.STALLED_WEIGHT);

        assertThat(chosen).isEmpty();
    }

    @Test
    void shouldAllowRpeButNotStalledWeight_whenThePlanChangeEndedTwoWeeksAgo() {
        // stalled weight looks at 3 weeks, so it stays quiet while the other rules may fire
        planChanges.endedAt = at("2026-10-25T23:00:00Z");

        check(Rule.STALLED_WEIGHT, Rule.HIGH_RPE);

        assertThat(dueRules()).containsExactly(Rule.HIGH_RPE);
    }

    @Test
    void shouldReportNothing_whenOnlyStalledWeightFiresTwoWeeksAfterAPlanChangeEnded() {
        planChanges.endedAt = at("2026-10-25T23:00:00Z");

        check(Rule.STALLED_WEIGHT);

        assertThat(chosen).isEmpty();
    }

    @Test
    void shouldAllowStalledWeight_whenThePlanChangeEndedThreeWeeksAgo() {
        planChanges.endedAt = at("2026-10-18T22:00:00Z");

        check(Rule.STALLED_WEIGHT);

        assertThat(dueRules()).containsExactly(Rule.STALLED_WEIGHT);
    }

    @Test
    void shouldCountFromTheNextMonday_whenThePlanChangeWasDeclinedMidWeek() {
        // declined on Wednesday 2026-10-28: the weeks begin to count on 2026-11-02, so two weeks have not passed
        planChanges.endedAt = at("2026-10-28T10:00:00Z");

        check(Rule.LOW_COMPLETION);

        assertThat(chosen).isEmpty();
    }

    @Test
    void shouldCountFromTheNextMonday_whenThePlanChangeWasDeclinedLaterOnAMonday() {
        // Monday 2026-10-26 at 14:00 in Warsaw: that Monday is already partly over, so counting starts on 2026-11-02
        planChanges.endedAt = at("2026-10-26T13:00:00Z");

        check(Rule.LOW_COMPLETION);

        assertThat(chosen).isEmpty();
    }

    @Test
    void shouldCountFromThatMonday_whenThePlanChangeEndedAtTheStartOfItsEffectiveWeek() {
        // accepted or expired: it ends when Monday 2026-10-26 begins in Warsaw
        planChanges.endedAt = at("2026-10-25T23:00:00Z");

        check(Rule.LOW_COMPLETION);

        assertThat(dueRules()).containsExactly(Rule.LOW_COMPLETION);
    }

    @Test
    void shouldIgnoreAnEndBeforeThePlan() {
        planChanges.endedAt = at("2026-01-05T10:00:00Z");

        check(Rule.STALLED_WEIGHT);

        assertThat(dueRules()).containsExactly(Rule.STALLED_WEIGHT);
    }

    private static Instant at(String instant) {
        return Instant.parse(instant);
    }
}
