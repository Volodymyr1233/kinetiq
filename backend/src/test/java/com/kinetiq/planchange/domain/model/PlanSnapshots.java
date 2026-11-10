package com.kinetiq.planchange.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Plans, due plan changes and content for the tests of limits, fallbacks and drafting. */
public final class PlanSnapshots {

    public static final LocalDate FIRST_WEEK = LocalDate.parse("2026-09-07");

    // limits never read the exercise of a move, so one stands for all
    private static final ExerciseId MOVED = ExerciseId.random();

    private PlanSnapshots() {}

    /** An entry counting repetitions at RPE 8 with 90 seconds of rest; {@code load} may be {@code null}. */
    public static EntrySnapshot entry(ExerciseId exercise, int sets, int amount, String load) {
        return new EntrySnapshot(
                exercise,
                Category.LEGS,
                Measure.REPS,
                sets,
                amount,
                load == null ? null : new BigDecimal(load),
                8,
                90);
    }

    /** An entry counting seconds, without a load. */
    public static EntrySnapshot timed(ExerciseId exercise, int sets, int seconds) {
        return new EntrySnapshot(exercise, Category.CORE, Measure.SECONDS, sets, seconds, null, 8, 60);
    }

    public static SessionSnapshot session(DayOfWeek day, EntrySnapshot... entries) {
        return new SessionSnapshot(day, "Session " + day.getValue(), List.of(entries));
    }

    /** A plan of 12 weeks from {@link #FIRST_WEEK}, version 3. */
    public static PlanSnapshot plan(SessionSnapshot... sessions) {
        return new PlanSnapshot(3, FIRST_WEEK, 12, List.of(sessions));
    }

    public static DuePlanChange due(Rule rule, ExerciseId... concerned) {
        return due(rule, List.of(), concerned);
    }

    public static DuePlanChange due(Rule rule, List<WeekTraining> weeks, ExerciseId... concerned) {
        return new DuePlanChange(
                AccountId.random(),
                ZoneId.of("Europe/Warsaw"),
                rule,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                Set.of(concerned),
                List.of(),
                weeks);
    }

    /** A change of one target of an entry of {@code plan}, from the value the plan has. */
    public static TargetChange change(PlanSnapshot plan, DayOfWeek day, int position, Target target, String value) {
        EntryRef ref = new EntryRef(day, position);
        EntrySnapshot entry = plan.entry(ref).orElseThrow();
        return new TargetChange(
                ref,
                entry.exercise(),
                plan.session(day).orElseThrow().name(),
                target,
                entry.value(target).orElse(BigDecimal.ZERO),
                new BigDecimal(value));
    }

    public static PlanChangeContent changes(TargetChange... changes) {
        return PlanChangeContent.ofChanges(Arrays.asList(changes));
    }

    public static PlanChangeContent removal(PlanSnapshot plan, DayOfWeek day, SessionMove... moves) {
        return new PlanChangeContent(
                List.of(),
                Optional.of(new RemovedSession(day, plan.session(day).map(SessionSnapshot::name).orElse("Gone"))),
                List.of(moves));
    }

    public static SessionMove move(DayOfWeek from, int position, DayOfWeek to) {
        return new SessionMove(new EntryRef(from, position), MOVED, to);
    }
}
