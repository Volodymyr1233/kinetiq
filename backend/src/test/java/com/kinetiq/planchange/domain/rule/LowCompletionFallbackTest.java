package com.kinetiq.planchange.domain.rule;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.RemovedSession;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LowCompletionFallbackTest {

    private static final ExerciseId SQUAT = ExerciseId.random();

    private final LowCompletionFallback fallback = new LowCompletionFallback();

    private final PlanSnapshot plan = plan(
            session(MONDAY, entry(SQUAT, 3, 8, "100.0")),
            session(TUESDAY, entry(SQUAT, 3, 8, "100.0")),
            session(WEDNESDAY, entry(SQUAT, 3, 8, "100.0")),
            session(FRIDAY, entry(SQUAT, 3, 8, "100.0")));

    private static WeekTraining week(String monday, DayOfWeek... finished) {
        return new WeekTraining(LocalDate.parse(monday), 4, 4, finished.length, List.of(), List.of(finished));
    }

    private Optional<DayOfWeek> removedDay(PlanSnapshot plan, WeekTraining... weeks) {
        return fallback.apply(plan, due(Rule.LOW_COMPLETION, List.of(weeks)))
                .flatMap(PlanChangeContent::removed)
                .map(RemovedSession::day);
    }

    @Test
    void shouldRemoveTheSessionFinishedLeastOften() {
        assertThat(removedDay(
                        plan,
                        week("2026-10-26", MONDAY, WEDNESDAY, FRIDAY),
                        week("2026-11-02", MONDAY, TUESDAY, WEDNESDAY, FRIDAY)))
                .contains(TUESDAY);
    }

    @Test
    void shouldRemoveTheLatestWeekday_whenSessionsTie() {
        assertThat(removedDay(plan, week("2026-10-26", MONDAY, WEDNESDAY), week("2026-11-02", MONDAY, WEDNESDAY)))
                .contains(FRIDAY);
    }

    @Test
    void shouldRemoveASessionThatWasNeverFinished_withItsNameAndNoMoves() {
        PlanChangeContent content = fallback.apply(
                        plan,
                        due(Rule.LOW_COMPLETION, List.of(
                                week("2026-10-26", MONDAY, TUESDAY, FRIDAY), week("2026-11-02", MONDAY, TUESDAY, FRIDAY))))
                .orElseThrow();

        assertThat(content.removed()).contains(new RemovedSession(WEDNESDAY, "Session 3"));
        assertThat(content.moves()).isEmpty();
        assertThat(content.changes()).isEmpty();
    }

    @Test
    void shouldChangeNothing_whenThePlanHasTwoSessions() {
        PlanSnapshot two = plan(session(MONDAY, entry(SQUAT, 3, 8, "100.0")), session(FRIDAY, entry(SQUAT, 3, 8, "100.0")));

        assertThat(removedDay(two, week("2026-10-26"), week("2026-11-02"))).isEmpty();
    }
}
