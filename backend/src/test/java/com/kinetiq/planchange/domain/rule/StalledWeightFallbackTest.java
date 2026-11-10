package com.kinetiq.planchange.domain.rule;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StalledWeightFallbackTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId BENCH = ExerciseId.random();
    private static final ExerciseId DIP = ExerciseId.random();

    private final StalledWeightFallback fallback = new StalledWeightFallback();

    @ParameterizedTest
    @CsvSource({"100.0, 105.0", "101.0, 106.5", "20.0, 21.0", "1.0, 1.5", "60.5, 64.0", "950.0, 997.5"})
    void shouldRaiseTheLoad5Percent_roundedUpToHalfAKilogram(String load, String expected) {
        // 101 x 1.05 = 106.05 rounds up to 106.5; 1 kg rises 50% to 1.5, exempt from the 15% limit
        PlanSnapshot plan = plan(session(MONDAY, entry(SQUAT, 3, 8, load)), session(FRIDAY));

        TargetChange change = fallback.apply(plan, due(Rule.STALLED_WEIGHT, SQUAT))
                .orElseThrow()
                .changes()
                .getFirst();

        assertThat(change.target()).isEqualTo(Target.LOAD_KG);
        assertThat(change.newValue()).isEqualByComparingTo(expected);
    }

    @Test
    void shouldLeaveALoad_whenTheRiseWouldPassTheEntryMaximum() {
        // 999 x 1.05 is far above 999.9 kg
        PlanSnapshot plan = plan(session(MONDAY, entry(SQUAT, 3, 8, "999.0")), session(FRIDAY));

        assertThat(fallback.apply(plan, due(Rule.STALLED_WEIGHT, SQUAT))).isEmpty();
    }

    @Test
    void shouldLeaveABodyweightEntryAndOtherExercisesAlone() {
        PlanSnapshot plan = plan(
                session(MONDAY, entry(SQUAT, 3, 8, "100.0"), entry(DIP, 3, 10, null), entry(BENCH, 3, 8, "60.0")),
                session(FRIDAY, entry(SQUAT, 3, 5, "120.0")));

        PlanChangeContent content = fallback.apply(plan, due(Rule.STALLED_WEIGHT, SQUAT, DIP)).orElseThrow();

        assertThat(content.changes())
                .extracting(TargetChange::entry)
                .containsExactly(new EntryRef(MONDAY, 0), new EntryRef(FRIDAY, 0));
        assertThat(content.changes()).allMatch(change -> change.target() == Target.LOAD_KG);
    }

    @Test
    void shouldChangeNothing_whenNoStalledEntryHasALoad() {
        PlanSnapshot plan = plan(session(MONDAY, entry(DIP, 3, 10, null)), session(FRIDAY));

        assertThat(fallback.apply(plan, due(Rule.STALLED_WEIGHT, DIP))).isEmpty();
    }
}
