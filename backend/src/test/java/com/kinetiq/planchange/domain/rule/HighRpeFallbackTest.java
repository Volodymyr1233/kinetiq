package com.kinetiq.planchange.domain.rule;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.timed;
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
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class HighRpeFallbackTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId ROW = ExerciseId.random();
    private static final ExerciseId PLANK = ExerciseId.random();

    private final HighRpeFallback fallback = new HighRpeFallback();

    private Optional<PlanChangeContent> apply(PlanSnapshot plan, ExerciseId... concerned) {
        return fallback.apply(plan, due(Rule.HIGH_RPE, concerned));
    }

    @ParameterizedTest
    @CsvSource({"10, 9", "5, 4", "2, 1", "12, 11", "20, 18", "30, 27"})
    void shouldTakeAtLeastOneRepetitionOff_atAbout90Percent(int amount, int expected) {
        // 5 to 4 is 20% less, beyond the 15% an LLM answer may take: the fallback is exempt
        PlanSnapshot plan = plan(session(MONDAY, entry(SQUAT, 3, amount, "100.0")), session(FRIDAY));

        TargetChange change = apply(plan, SQUAT).orElseThrow().changes().getFirst();

        assertThat(change.target()).isEqualTo(Target.AMOUNT);
        assertThat(change.oldValue()).isEqualByComparingTo(BigDecimal.valueOf(amount));
        assertThat(change.newValue()).isEqualByComparingTo(BigDecimal.valueOf(expected));
    }

    @Test
    void shouldChangeNothing_whenTheOnlyEntryHasOneRepetition() {
        PlanSnapshot plan = plan(session(MONDAY, entry(SQUAT, 3, 1, "200.0")), session(FRIDAY));

        assertThat(apply(plan, SQUAT)).isEmpty();
    }

    @Test
    void shouldChangeEveryEntryOfAConcernedExercise_andNoOther() {
        PlanSnapshot plan = plan(
                session(MONDAY, entry(SQUAT, 3, 10, "100.0"), entry(ROW, 3, 10, "50.0")),
                session(FRIDAY, entry(SQUAT, 4, 8, "90.0")));

        PlanChangeContent content = apply(plan, SQUAT).orElseThrow();

        assertThat(content.changes())
                .extracting(TargetChange::entry)
                .containsExactly(new EntryRef(MONDAY, 0), new EntryRef(FRIDAY, 0));
        assertThat(content.changes()).allMatch(change -> change.target() == Target.AMOUNT);
        assertThat(content.removed()).isEmpty();
    }

    @Test
    void shouldLowerSeconds_forAnExerciseThatCountsThem() {
        PlanSnapshot plan = plan(session(MONDAY, timed(PLANK, 3, 30)), session(FRIDAY));

        assertThat(apply(plan, PLANK).orElseThrow().changes().getFirst().newValue()).isEqualByComparingTo("27");
    }
}
