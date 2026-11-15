package com.kinetiq.planchange.domain.model;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.change;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.changes;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.removal;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanSnapshotFitTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId PRESS = ExerciseId.random();
    private static final ExerciseId ROW = ExerciseId.random();

    private final PlanSnapshot original = plan(
            session(MONDAY, entry(SQUAT, 4, 8, "80.0"), entry(PRESS, 3, 10, "40.0")),
            session(WEDNESDAY, entry(ROW, 3, 10, null)),
            session(FRIDAY, entry(SQUAT, 3, 5, "90.0")));

    private PlanSnapshot withSessions(SessionSnapshot... sessions) {
        return new PlanSnapshot(original.version() + 1, original.firstWeek(), original.weeks(), List.of(sessions));
    }

    private SessionSnapshot original(java.time.DayOfWeek day) {
        return original.session(day).orElseThrow();
    }

    @Test
    void shouldFit_whenThePlanIsTheOneTheContentWasMadeFrom() {
        PlanChangeContent content = changes(change(original, MONDAY, 0, Target.SETS, "3"));

        assertThat(original.stillFits(content)).isTrue();
    }

    @Test
    void shouldFit_whenAnUnrelatedEntryWasEdited() {
        PlanChangeContent content = changes(change(original, MONDAY, 0, Target.SETS, "3"));
        PlanSnapshot plan = withSessions(
                session(MONDAY, entry(SQUAT, 4, 8, "80.0"), entry(PRESS, 5, 10, "40.0")),
                original(WEDNESDAY),
                original(FRIDAY));

        assertThat(plan.stillFits(content)).isTrue();
    }

    @Test
    void shouldNotFit_whenATouchedValueWasEdited() {
        PlanChangeContent content = changes(change(original, MONDAY, 0, Target.SETS, "3"));
        PlanSnapshot plan = withSessions(
                session(MONDAY, entry(SQUAT, 5, 8, "80.0"), entry(PRESS, 3, 10, "40.0")),
                original(WEDNESDAY),
                original(FRIDAY));

        assertThat(plan.stillFits(content)).isFalse();
    }

    @Test
    void shouldNotFit_whenTheEntryAtThePlaceIsAnotherExercise() {
        PlanChangeContent content = changes(change(original, MONDAY, 0, Target.SETS, "3"));
        PlanSnapshot plan = withSessions(
                session(MONDAY, entry(PRESS, 4, 8, "80.0"), entry(PRESS, 3, 10, "40.0")),
                original(WEDNESDAY),
                original(FRIDAY));

        assertThat(plan.stillFits(content)).isFalse();
    }

    @Test
    void shouldNotFit_whenTheTouchedEntryWasRemoved() {
        PlanChangeContent content = changes(change(original, MONDAY, 1, Target.SETS, "2"));
        PlanSnapshot plan =
                withSessions(session(MONDAY, entry(SQUAT, 4, 8, "80.0")), original(WEDNESDAY), original(FRIDAY));

        assertThat(plan.stillFits(content)).isFalse();
    }

    @Test
    void shouldNotFit_whenTheLoadOfATouchedEntryWasRemoved() {
        PlanChangeContent content = changes(change(original, MONDAY, 0, Target.LOAD_KG, "77.5"));
        PlanSnapshot plan = withSessions(
                session(MONDAY, entry(SQUAT, 4, 8, null), entry(PRESS, 3, 10, "40.0")),
                original(WEDNESDAY),
                original(FRIDAY));

        assertThat(plan.stillFits(content)).isFalse();
    }

    @Test
    void shouldNotFit_whenTheRemovedSessionIsGone() {
        PlanChangeContent content = removal(original, WEDNESDAY);
        PlanSnapshot plan = withSessions(original(MONDAY), original(FRIDAY));

        assertThat(original.stillFits(content)).isTrue();
        assertThat(plan.stillFits(content)).isFalse();
    }

    @Test
    void shouldNotFit_whenTheRemovedSessionWasRenamed() {
        PlanChangeContent content = removal(original, WEDNESDAY);
        PlanSnapshot plan = withSessions(
                original(MONDAY),
                new SessionSnapshot(WEDNESDAY, "Renamed", original(WEDNESDAY).entries()),
                original(FRIDAY));

        assertThat(plan.stillFits(content)).isFalse();
    }

    @Test
    void shouldNotFit_whenAMovedEntryIsNoLongerAtItsPlace() {
        PlanChangeContent content = new PlanChangeContent(
                List.of(),
                Optional.of(new RemovedSession(WEDNESDAY, original(WEDNESDAY).name())),
                List.of(new SessionMove(new EntryRef(WEDNESDAY, 0), ROW, FRIDAY)));
        PlanSnapshot swapped = withSessions(
                original(MONDAY),
                new SessionSnapshot(WEDNESDAY, original(WEDNESDAY).name(), List.of(entry(PRESS, 3, 10, null))),
                original(FRIDAY));

        assertThat(original.stillFits(content)).isTrue();
        assertThat(swapped.stillFits(content)).isFalse();
    }
}
