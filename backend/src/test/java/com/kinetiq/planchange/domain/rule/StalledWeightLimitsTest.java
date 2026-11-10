package com.kinetiq.planchange.domain.rule;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.change;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.changes;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.removal;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StalledWeightLimitsTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId BENCH = ExerciseId.random();
    private static final ExerciseId PLANK = ExerciseId.random();

    private final PlanSnapshot plan = plan(
            session(MONDAY, entry(SQUAT, 3, 8, "100.0"), entry(PLANK, 3, 30, null)),
            session(WEDNESDAY, entry(BENCH, 3, 8, "60.0")),
            session(FRIDAY, entry(BENCH, 3, 8, "60.0")));

    private final StalledWeightLimits limits = new StalledWeightLimits();

    private Set<String> violations(PlanChangeContent content) {
        return limits.violations(plan, due(Rule.STALLED_WEIGHT, SQUAT, PLANK), content);
    }

    private Set<String> squatTo(String load) {
        return violations(changes(change(plan, MONDAY, 0, Target.LOAD_KG, load)));
    }

    @Test
    void shouldAccept_whenTheLoadRisesExactly2Point5Percent_and15Percent() {
        assertThat(squatTo("102.5")).isEmpty();
        assertThat(squatTo("115.0")).isEmpty();
    }

    @Test
    void shouldRefuse_whenTheLoadRisesJustOutsideTheRange() {
        assertThat(squatTo("102.0")).containsExactly(LimitCode.LOAD_OUT_OF_RANGE);
        assertThat(squatTo("115.5")).containsExactly(LimitCode.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldRefuse_whenTheLoadIsNotAMultipleOfHalfAKilogram() {
        assertThat(squatTo("105.2")).containsExactly(LimitCode.LOAD_NOT_HALF_KG);
    }

    @Test
    void shouldJudgeTheRangeOnTheRoundedValue() {
        // 40 kg: +2.5% is 41.0 exactly, while 40.5 is only +1.25%
        PlanSnapshot light = plan(session(MONDAY, entry(SQUAT, 3, 8, "40.0")), session(FRIDAY));
        Set<String> exact =
                limits.violations(light, due(Rule.STALLED_WEIGHT, SQUAT), changes(change(light, MONDAY, 0, Target.LOAD_KG, "41.0")));
        Set<String> below =
                limits.violations(light, due(Rule.STALLED_WEIGHT, SQUAT), changes(change(light, MONDAY, 0, Target.LOAD_KG, "40.5")));

        assertThat(exact).isEmpty();
        assertThat(below).containsExactly(LimitCode.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldRefuse_whenALoadIsLowered() {
        assertThat(squatTo("95.0")).containsExactly(LimitCode.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldRefuse_whenAStalledEntryWithALoadIsLeftOut() {
        assertThat(violations(changes(change(plan, MONDAY, 0, Target.AMOUNT, "7"))))
                .containsExactly(LimitCode.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldRefuse_whenAnExerciseThatIsNotStalledRises() {
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.LOAD_KG, "105.0"),
                        change(plan, WEDNESDAY, 0, Target.LOAD_KG, "63.0"))))
                .containsExactly(LimitCode.ENTRY_NOT_CONCERNED);
    }

    @Test
    void shouldAcceptADropOfUpTo2Repetitions_butNotMore_norARise() {
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.LOAD_KG, "105.0"), change(plan, MONDAY, 0, Target.AMOUNT, "6"))))
                .isEmpty();
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.LOAD_KG, "105.0"), change(plan, MONDAY, 0, Target.AMOUNT, "5"))))
                .containsExactly(LimitCode.AMOUNT_DROP_TOO_LARGE);
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.LOAD_KG, "105.0"), change(plan, MONDAY, 0, Target.AMOUNT, "9"))))
                .containsExactly(LimitCode.AMOUNT_DROP_TOO_LARGE);
    }

    @Test
    void shouldNeverDropRepetitionsBelow1() {
        PlanSnapshot heavy = plan(session(MONDAY, entry(SQUAT, 5, 2, "150.0")), session(FRIDAY));

        assertThat(limits.violations(
                        heavy,
                        due(Rule.STALLED_WEIGHT, SQUAT),
                        changes(
                                change(heavy, MONDAY, 0, Target.LOAD_KG, "155.0"),
                                change(heavy, MONDAY, 0, Target.AMOUNT, "0"))))
                .containsExactly(LimitCode.AMOUNT_DROP_TOO_LARGE);
    }

    @Test
    void shouldRefuseOtherTargets_andASessionRemoval() {
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.LOAD_KG, "105.0"), change(plan, MONDAY, 0, Target.SETS, "4"))))
                .containsExactly(LimitCode.TARGET_NOT_ALLOWED);
        assertThat(violations(removal(plan, FRIDAY))).contains(LimitCode.SESSION_REMOVAL_COUNT);
    }
}
