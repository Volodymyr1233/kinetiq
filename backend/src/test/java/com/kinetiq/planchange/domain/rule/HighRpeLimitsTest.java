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

class HighRpeLimitsTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId ROW = ExerciseId.random();

    // the squat entry has a volume of 4 x 10 = 40
    private final PlanSnapshot plan = plan(
            session(MONDAY, entry(SQUAT, 4, 10, "100.0"), entry(ROW, 3, 10, "50.0")),
            session(WEDNESDAY, entry(ROW, 3, 10, "50.0")),
            session(FRIDAY, entry(SQUAT, 2, 10, "80.0")));

    private final HighRpeLimits limits = new HighRpeLimits();

    private Set<String> violations(PlanChangeContent content) {
        return limits.violations(plan, due(Rule.HIGH_RPE, SQUAT), content);
    }

    @Test
    void shouldAccept_whenTheVolumeDropsBy5Percent() {
        // 40 to 38 is 95%
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.SETS, "2"), change(plan, MONDAY, 0, Target.AMOUNT, "19"))))
                .isEmpty();
    }

    @Test
    void shouldAccept_whenTheVolumeDropsBy15Percent() {
        // 40 to 34 is 85%
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.SETS, "2"), change(plan, MONDAY, 0, Target.AMOUNT, "17"))))
                .isEmpty();
    }

    @Test
    void shouldRefuse_whenTheVolumeDropsByLessThan5Percent() {
        // 40 to 39 is 97.5%
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.SETS, "3"), change(plan, MONDAY, 0, Target.AMOUNT, "13"))))
                .containsExactly(LimitCode.VOLUME_OUT_OF_RANGE);
    }

    @Test
    void shouldRefuse_whenTheVolumeDropsByMoreThan15Percent() {
        // 40 to 33 is 82.5%; 40 to 36 by one repetition less is 90% and passes
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.SETS, "3"), change(plan, MONDAY, 0, Target.AMOUNT, "11"))))
                .containsExactly(LimitCode.VOLUME_OUT_OF_RANGE);
        assertThat(violations(changes(change(plan, MONDAY, 0, Target.AMOUNT, "9")))).isEmpty();
    }

    @Test
    void shouldJudgeEachEntryOnItsOwn_whenAnExerciseIsInTwoSessions() {
        // Friday's 2 x 10 to 2 x 8 is 80%, even though both entries together drop 13%
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.AMOUNT, "9"), change(plan, FRIDAY, 0, Target.AMOUNT, "8"))))
                .containsExactly(LimitCode.VOLUME_OUT_OF_RANGE);
    }

    @Test
    void shouldRefuse_whenAnExerciseNotAboveRpe8Changes() {
        assertThat(violations(changes(change(plan, WEDNESDAY, 0, Target.AMOUNT, "9"))))
                .containsExactly(LimitCode.ENTRY_NOT_CONCERNED);
    }

    @Test
    void shouldRefuse_whenTheLoadOrRpeChanges() {
        assertThat(violations(changes(change(plan, MONDAY, 0, Target.LOAD_KG, "90.0"))))
                .containsExactly(LimitCode.TARGET_NOT_ALLOWED);
        assertThat(violations(changes(
                        change(plan, MONDAY, 0, Target.AMOUNT, "9"), change(plan, MONDAY, 0, Target.RPE, "7"))))
                .contains(LimitCode.TARGET_NOT_ALLOWED);
    }

    @Test
    void shouldRefuse_whenASessionIsRemoved() {
        assertThat(violations(removal(plan, FRIDAY))).containsExactly(LimitCode.SESSION_REMOVAL_COUNT);
    }
}
