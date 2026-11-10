package com.kinetiq.planchange.domain.service;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.LoggedSet;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.rule.HighRpeRule;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A fired rule ends in a stored plan change. The client trains a plan from 2026-09-07 and is checked on Monday
 * 2026-11-09 in Warsaw, after two weeks with every set at RPE 9; the drafting service is not configured.
 */
class PlanChangeCreationTest {

    private static final ExerciseId SQUAT = ExerciseId.random();

    private final FakeTrainingHistory history = new FakeTrainingHistory();
    private final FakePlanChangeRepository planChanges = new FakePlanChangeRepository();
    private final FakePlanSource plans = new FakePlanSource();
    private final MutableClock clock = new MutableClock("2026-11-09T05:00:00Z");
    private final AccountId client = history.publishedPlan("Europe/Warsaw");

    private final PlanChangeDetectionService service = new PlanChangeDetectionService(
            history, planChanges, List.of(new HighRpeRule()), plans, new FakePlanChangeDrafter().drafting(), clock);

    PlanChangeCreationTest() {
        for (String monday : List.of("2026-10-26", "2026-11-02")) {
            history.trained(new WeekTraining(
                    LocalDate.parse(monday), 2, 2, 2, List.of(new LoggedSet(SQUAT, null, 9)), List.of(MONDAY, FRIDAY)));
        }
    }

    private static PlanSnapshot planOfWeeks(int weeks) {
        return new PlanSnapshot(
                7,
                LocalDate.parse("2026-09-07"),
                weeks,
                List.of(session(MONDAY, entry(SQUAT, 3, 10, "100.0")), session(FRIDAY, entry(SQUAT, 3, 10, "100.0"))));
    }

    @Test
    void shouldStoreOnePendingPlanChange_withTheFixedContentNumbersAndPlanVersion() {
        plans.plans.put(client, planOfWeeks(12));

        service.check(client);

        assertThat(planChanges.saved).hasSize(1);
        PlanChange stored = planChanges.saved.getFirst();
        assertThat(stored.getClient()).isEqualTo(client);
        assertThat(stored.getRule()).isEqualTo(Rule.HIGH_RPE);
        assertThat(stored.getState()).isEqualTo(PlanChangeState.PENDING);
        assertThat(stored.getPlanVersion()).isEqualTo(7);
        assertThat(stored.getEffectiveWeek()).isEqualTo(LocalDate.parse("2026-11-16"));
        assertThat(stored.getWindowStart()).isEqualTo(LocalDate.parse("2026-10-26"));
        assertThat(stored.getFigures()).extracting(figure -> figure.value()).containsExactly(
                new BigDecimal("9.000"), new BigDecimal("9.000"));
        assertThat(stored.getConcernedExercises()).containsExactly(SQUAT);
        assertThat(stored.getContent().changes()).hasSize(2);
    }

    @Test
    void shouldStoreNothing_whenThePlanEndsBeforeTheEffectiveWeek() {
        // 10 weeks from 2026-09-07 end with the week of 2026-11-09
        plans.plans.put(client, planOfWeeks(10));

        service.check(client);

        assertThat(planChanges.saved).isEmpty();
    }

    @Test
    void shouldStoreOnTheLastPlanWeek_whenItIsTheEffectiveWeek() {
        plans.plans.put(client, planOfWeeks(11));

        service.check(client);

        assertThat(planChanges.saved).hasSize(1);
    }

    @Test
    void shouldStoreNothing_whenTheClientHasNoPublishedPlanAnyMore() {
        service.check(client);

        assertThat(planChanges.saved).isEmpty();
    }

    @Test
    void shouldNotEvaluate_whenTheClientHasAnOpenPlanChange() {
        plans.plans.put(client, planOfWeeks(12));
        planChanges.open = true;

        service.check(client);

        assertThat(planChanges.saved).isEmpty();
        assertThat(history.weekReads).isZero();
    }

    @Test
    void shouldStoreOne_whenTheSameWeekIsCheckedTwice() {
        plans.plans.put(client, planOfWeeks(12));

        service.check(client);
        clock.set("2026-11-11T05:00:00Z");
        service.check(client);

        assertThat(planChanges.saved).hasSize(1);
    }
}
