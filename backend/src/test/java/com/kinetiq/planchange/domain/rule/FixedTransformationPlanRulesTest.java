package com.kinetiq.planchange.domain.rule;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.timed;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Every fixed transformation keeps the plan rules and gives the same content for the same input. */
class FixedTransformationPlanRulesTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId PLANK = ExerciseId.random();

    private static final PlanSnapshot PLAN = plan(
            session(MONDAY, entry(SQUAT, 3, 5, "1.0"), timed(PLANK, 3, 2)),
            session(WEDNESDAY, entry(SQUAT, 4, 2, "999.0")),
            session(FRIDAY, entry(SQUAT, 3, 1, "101.0")));

    private static final List<WeekTraining> WEEKS = List.of(
            new WeekTraining(LocalDate.parse("2026-10-26"), 3, 3, 1, List.of(), List.of(MONDAY)),
            new WeekTraining(LocalDate.parse("2026-11-02"), 3, 3, 1, List.of(), List.of(FRIDAY)));

    static Stream<Arguments> fallbacks() {
        return Stream.of(
                Arguments.of(new HighRpeFallback(), due(Rule.HIGH_RPE, SQUAT, PLANK)),
                Arguments.of(new StalledWeightFallback(), due(Rule.STALLED_WEIGHT, SQUAT, PLANK)),
                Arguments.of(new LowCompletionFallback(), due(Rule.LOW_COMPLETION, WEEKS)));
    }

    @ParameterizedTest
    @MethodSource("fallbacks")
    void shouldKeepThePlanRules_andGiveTheSameContentTwice(FixedTransformation fallback, DuePlanChange due) {
        PlanChangeContent first = fallback.apply(PLAN, due).orElseThrow();
        PlanChangeContent second = fallback.apply(PLAN, due).orElseThrow();

        assertThat(new PlanRuleLimits().violations(PLAN, first)).isEmpty();
        assertThat(second).isEqualTo(first);
    }
}
