package com.kinetiq.planchange.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * A suggested lasting modification of one client's plan, with the rule that caused it, the numbers it compared and the
 * checked content. It takes effect from its effective week, once trainer and client have accepted it.
 */
@Getter
public class PlanChange {

    private final PlanChangeId id;
    private final AccountId client;
    private final Rule rule;
    private final PlanChangeState state;
    private final long planVersion;
    private final LocalDate windowStart;
    private final LocalDate windowEnd;
    private final LocalDate effectiveWeek;
    private final PlanChangeContent content;
    private final List<Figure> figures;
    private final Set<ExerciseId> concernedExercises;
    private final Instant createdAt;

    @Getter(AccessLevel.NONE)
    private final Instant endedAt;

    /** Rebuilds a stored plan change; {@code endedAt} is {@code null} while it is open. */
    public PlanChange(
            PlanChangeId id,
            AccountId client,
            Rule rule,
            PlanChangeState state,
            long planVersion,
            LocalDate windowStart,
            LocalDate windowEnd,
            LocalDate effectiveWeek,
            PlanChangeContent content,
            List<Figure> figures,
            Set<ExerciseId> concernedExercises,
            Instant createdAt,
            Instant endedAt) {
        this.id = Objects.requireNonNull(id);
        this.client = Objects.requireNonNull(client);
        this.rule = Objects.requireNonNull(rule);
        this.state = Objects.requireNonNull(state);
        this.planVersion = planVersion;
        this.windowStart = Objects.requireNonNull(windowStart);
        this.windowEnd = Objects.requireNonNull(windowEnd);
        if (Objects.requireNonNull(effectiveWeek).getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new IllegalArgumentException("the effective week starts on a Monday");
        }
        this.effectiveWeek = effectiveWeek;
        this.content = Objects.requireNonNull(content);
        this.figures = List.copyOf(figures);
        this.concernedExercises = Set.copyOf(concernedExercises);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.endedAt = endedAt;
    }

    /**
     * Creates the pending plan change of a due one, made from the plan of version {@code planVersion}. Its effective
     * week is the Monday after the day of {@code now} in the client's time zone.
     */
    public static PlanChange create(DuePlanChange due, PlanChangeContent content, long planVersion, Instant now) {
        return new PlanChange(
                PlanChangeId.random(),
                due.client(),
                due.rule(),
                PlanChangeState.PENDING,
                planVersion,
                due.windowStart(),
                due.windowEnd(),
                effectiveWeek(now, due.zone()),
                content,
                due.figures(),
                due.exercises(),
                now,
                null);
    }

    /** The Monday after the day of {@code now} in {@code zone}; on a Monday it is the next one. */
    public static LocalDate effectiveWeek(Instant now, ZoneId zone) {
        return now.atZone(zone).toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    public boolean isOpen() {
        return state.isOpen();
    }

    /** When the plan change ended; empty while it is open. */
    public Optional<Instant> getEndedAt() {
        return Optional.ofNullable(endedAt);
    }
}
