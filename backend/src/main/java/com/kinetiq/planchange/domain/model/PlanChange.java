package com.kinetiq.planchange.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.exception.PlanChangeNothingToUndoException;
import com.kinetiq.planchange.domain.exception.PlanChangeWeekBegunException;
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

    @Getter(AccessLevel.NONE)
    private final Instant trainerAcceptedAt;

    @Getter(AccessLevel.NONE)
    private final Instant clientAcceptedAt;

    @Getter(AccessLevel.NONE)
    private final Decision decision;

    /**
     * Rebuilds a stored plan change; {@code endedAt} is {@code null} while it is open, an acceptance time while that
     * person has not accepted, and {@code decision} while it is not declined.
     */
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
            Instant endedAt,
            Instant trainerAcceptedAt,
            Instant clientAcceptedAt,
            Decision decision) {
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
        this.trainerAcceptedAt = trainerAcceptedAt;
        this.clientAcceptedAt = clientAcceptedAt;
        this.decision = decision;
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
                null,
                null,
                null,
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

    /** When the trainer accepted; empty while they have not. */
    public Optional<Instant> getTrainerAcceptedAt() {
        return Optional.ofNullable(trainerAcceptedAt);
    }

    /** When the client accepted; empty while they have not. */
    public Optional<Instant> getClientAcceptedAt() {
        return Optional.ofNullable(clientAcceptedAt);
    }

    /** How a declined plan change ended; empty for any other. */
    public Optional<Decision> getDecision() {
        return Optional.ofNullable(decision);
    }

    public boolean acceptedBy(Side side) {
        return acceptedAt(side) != null;
    }

    /**
     * Records the acceptance of {@code side}; when the other side accepted already, the plan change is applied. A
     * second acceptance by the same side changes nothing.
     *
     * @param today the day it is for the client, who lives in their own time zone
     */
    public PlanChange accept(Side side, Instant now, LocalDate today) {
        requireDecidable(today);
        if (acceptedBy(side)) {
            return this;
        }
        Instant trainerAt = side == Side.TRAINER ? now : trainerAcceptedAt;
        Instant clientAt = side == Side.CLIENT ? now : clientAcceptedAt;
        boolean both = trainerAt != null && clientAt != null;
        PlanChangeState next = both ? PlanChangeState.APPLIED : PlanChangeState.ACCEPTED_BY_ONE;
        return with(next, both ? now : null, trainerAt, clientAt, null);
    }

    /**
     * Takes back the acceptance of {@code side}, while the other side has not accepted and the effective week has not
     * begun.
     */
    public PlanChange undo(Side side, LocalDate today) {
        requireDecidable(today);
        if (state != PlanChangeState.ACCEPTED_BY_ONE || !acceptedBy(side)) {
            throw new PlanChangeNothingToUndoException();
        }
        return with(PlanChangeState.PENDING, null, null, null, null);
    }

    /** Ends the plan change as declined: {@code side} keeps the current plan for {@code reason}. */
    public PlanChange decline(Side side, DeclineReason reason, String note, Instant now, LocalDate today) {
        requireDecidable(today);
        return with(PlanChangeState.DECLINED, now, trainerAcceptedAt, clientAcceptedAt, new Decision(side, reason, note));
    }

    private Instant acceptedAt(Side side) {
        return side == Side.TRAINER ? trainerAcceptedAt : clientAcceptedAt;
    }

    private void requireDecidable(LocalDate today) {
        if (!state.isOpen()) {
            throw new PlanChangeEndedException();
        }
        if (!today.isBefore(effectiveWeek)) {
            throw new PlanChangeWeekBegunException();
        }
    }

    private PlanChange with(PlanChangeState newState, Instant newEndedAt, Instant trainerAt, Instant clientAt, Decision newDecision) {
        return new PlanChange(
                id, this.client, rule, newState, planVersion, windowStart, windowEnd, effectiveWeek, content, figures,
                concernedExercises, createdAt, newEndedAt, trainerAt, clientAt, newDecision);
    }
}
