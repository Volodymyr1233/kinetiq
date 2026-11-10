package com.kinetiq.planchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.rule.PlanChangeRule;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanChangeDetectionServiceTest {

    private final FakeTrainingHistory history = new FakeTrainingHistory();
    private final MutableClock clock = new MutableClock("2026-11-09T01:00:00Z");
    private final PlanChangeDetectionService service = new PlanChangeDetectionService(
            history, new FakePlanChangeRepository(), List.of(), new FakePlanSource(), new FakePlanChangeDrafter().drafting(), clock);

    private void checkAllDue() {
        for (AccountId client : service.due()) {
            service.check(client);
        }
    }

    private static List<LocalDate> weeks(String... mondays) {
        return Arrays.stream(mondays).map(LocalDate::parse).toList();
    }

    @Test
    void shouldCheckOnlyTheClientWhoseMondayHasBegun_whenZonesDiffer() {
        AccountId warsaw = history.publishedPlan("Europe/Warsaw");
        AccountId losAngeles = history.publishedPlan("America/Los_Angeles");
        history.checked(warsaw, "2026-11-02");
        history.checked(losAngeles, "2026-11-02");

        clock.set("2026-11-09T01:59:00Z");
        assertThat(service.due()).isEmpty();

        clock.set("2026-11-09T02:00:00Z");
        assertThat(service.due()).containsExactly(warsaw);

        clock.set("2026-11-09T11:00:00Z");
        assertThat(service.due()).containsExactlyInAnyOrder(warsaw, losAngeles);
    }

    @Test
    void shouldCheckOncePerWeek_whenCheckIsCalledAgain() {
        AccountId client = history.publishedPlan("Europe/Warsaw");
        clock.set("2026-11-09T02:00:00Z");

        service.check(client);
        clock.advance(Duration.ofDays(3));
        service.check(client);

        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2026-11-09"));
        assertThat(history.claims).hasSize(1);
    }

    @Test
    void shouldCheckLaterInTheWeek_whenTheRunWasMissedAtMonday0300() {
        AccountId client = history.publishedPlan("Europe/Warsaw");
        history.checked(client, "2026-11-02");

        clock.set("2026-11-13T17:00:00Z");
        checkAllDue();

        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2026-11-02", "2026-11-09"));
    }

    @Test
    void shouldCheckOnceInTheWeekOfAClockChange_whenMondayFollowsIt() {
        AccountId client = history.publishedPlan("Europe/Warsaw");
        history.checked(client, "2027-03-22");

        // Sunday 2027-03-28 loses an hour, so Monday 03:00 is 01:00Z
        clock.set("2027-03-29T00:59:00Z");
        checkAllDue();
        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2027-03-22"));

        clock.set("2027-03-29T01:00:00Z");
        checkAllDue();
        checkAllDue();
        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2027-03-22", "2027-03-29"));
    }

    @Test
    void shouldNotCheckAgain_whenTheClientMovedWestAfterTheWeekWasClaimed() {
        // claimed for 2026-11-09 as a Warsaw client; in Los Angeles that week has not begun at 02:00Z
        AccountId client = history.publishedPlan("America/Los_Angeles");
        history.checked(client, "2026-11-09");
        clock.set("2026-11-09T02:00:00Z");

        service.check(client);

        assertThat(history.claims).isEmpty();
        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2026-11-09"));
    }

    @Test
    void shouldSkipTheClient_whenThereIsNoPublishedPlan() {
        service.check(AccountId.random());

        assertThat(history.claims).isEmpty();
    }

    @Test
    void shouldCheckEachClientExactlyOncePerWeek_overAMonth() {
        AccountId warsaw = history.publishedPlan("Europe/Warsaw");
        AccountId losAngeles = history.publishedPlan("America/Los_Angeles");
        history.checked(warsaw, "2026-10-26");
        history.checked(losAngeles, "2026-10-26");
        clock.set("2026-11-02T00:00:00Z");

        // a tick every 10 minutes for 28 days, as the sweep runs
        for (int tick = 0; tick < 28 * 24 * 6; tick++) {
            checkAllDue();
            clock.advance(Duration.ofMinutes(10));
        }

        List<LocalDate> mondays = weeks("2026-10-26", "2026-11-02", "2026-11-09", "2026-11-16", "2026-11-23");
        assertThat(history.claimedWeeks(warsaw)).isEqualTo(mondays);
        assertThat(history.claimedWeeks(losAngeles)).isEqualTo(mondays);
        assertThat(history.claims).hasSize(8);
    }

    /** Records the windows it is asked about. */
    private static final class RecordingRule implements PlanChangeRule {

        private final Rule rule;
        final List<List<LocalDate>> windows = new ArrayList<>();

        RecordingRule(Rule rule) {
            this.rule = rule;
        }

        @Override
        public Rule rule() {
            return rule;
        }

        @Override
        public Optional<Finding> evaluate(List<WeekTraining> window) {
            windows.add(window.stream().map(WeekTraining::monday).toList());
            return Optional.empty();
        }
    }

    private List<List<LocalDate>> windowsAskedAt(String instant, Rule... rules) {
        List<RecordingRule> recording = Arrays.stream(rules).map(RecordingRule::new).toList();
        AccountId client = history.publishedPlan("Europe/Warsaw");
        clock.set(instant);
        new PlanChangeDetectionService(
                        history,
                        new FakePlanChangeRepository(),
                        List.copyOf(recording),
                        new FakePlanSource(),
                        new FakePlanChangeDrafter().drafting(),
                        clock).check(client);
        return recording.stream().flatMap(rule -> rule.windows.stream()).toList();
    }

    @Test
    void shouldAskARuleForItsOwnWindow_oldestFirst() {
        // check week 2026-10-12 completes the weeks of 2026-10-05 and before; the plan starts 2026-09-07
        assertThat(windowsAskedAt("2026-10-12T05:00:00Z", Rule.HIGH_RPE, Rule.STALLED_WEIGHT))
                .containsExactly(weeks("2026-09-28", "2026-10-05"), weeks("2026-09-21", "2026-09-28", "2026-10-05"));
    }

    @Test
    void shouldNotEvaluateARule_whenItsWindowStartsBeforeThePlan() {
        // the completed weeks are 2026-09-07 only: neither a 2 nor a 3 week window fits
        assertThat(windowsAskedAt("2026-09-14T05:00:00Z", Rule.HIGH_RPE, Rule.STALLED_WEIGHT)).isEmpty();
        assertThat(history.weekReads).isZero();
    }

    @Test
    void shouldEvaluateOnlyTheRulesWhoseWindowFits_whenThePlanIsTwoWeeksOld() {
        assertThat(windowsAskedAt("2026-09-21T05:00:00Z", Rule.HIGH_RPE, Rule.STALLED_WEIGHT))
                .containsExactly(weeks("2026-09-07", "2026-09-14"));
    }

    @Test
    void shouldEvaluateTheLastPlanWeek_butNotTheWeekAfterThePlan() {
        // the plan has 10 weeks from 2026-09-07, so its last week begins 2026-11-09
        assertThat(windowsAskedAt("2026-11-16T05:00:00Z", Rule.HIGH_RPE)).containsExactly(weeks("2026-11-02", "2026-11-09"));
        assertThat(windowsAskedAt("2026-11-23T05:00:00Z", Rule.HIGH_RPE)).isEmpty();
    }
}
