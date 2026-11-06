package com.kinetiq.plan.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.exception.PlanAlreadyPublishedException;
import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import com.kinetiq.plan.domain.exception.PlanWeeksBeforeCurrentException;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * The training prescribed for one client. A plan with a first week is published; the first week is the Monday of
 * plan week 1. Sessions are kept in week order, Monday first. Every plan keeps the limits checked here; a draft may
 * still be incomplete, see {@link #requireComplete()}.
 */
@Getter
public class Plan {

    public static final int MIN_WEEKS = 4;
    public static final int MAX_WEEKS = 16;
    public static final int MIN_SESSIONS_WHEN_COMPLETE = 2;
    public static final int MAX_SESSIONS = 6;

    private static final int DAYS_PER_WEEK = 7;

    private final PlanId id;
    private final AccountId client;
    private final Goal goal;
    private final int weeks;

    @Getter(AccessLevel.NONE)
    private final LocalDate firstWeek;

    private final List<Session> sessions;
    private final long version;

    @Getter(AccessLevel.NONE)
    private final AutomaticDraft automaticDraft;

    /** Rebuilds a stored plan without a mark; {@code firstWeek} is {@code null} for a draft. */
    public Plan(
            PlanId id,
            AccountId client,
            Goal goal,
            int weeks,
            LocalDate firstWeek,
            List<Session> sessions,
            long version) {
        this(id, client, goal, weeks, firstWeek, sessions, version, null);
    }

    /** Rebuilds a stored plan; {@code firstWeek} and {@code automaticDraft} may be {@code null}. */
    public Plan(
            PlanId id,
            AccountId client,
            Goal goal,
            int weeks,
            LocalDate firstWeek,
            List<Session> sessions,
            long version,
            AutomaticDraft automaticDraft) {
        if (weeks < MIN_WEEKS || weeks > MAX_WEEKS) {
            throw new PlanRuleViolationException("plan.weeks-out-of-range");
        }
        if (sessions.size() > MAX_SESSIONS) {
            throw new PlanRuleViolationException("plan.too-many-sessions");
        }
        if (sessions.stream().map(Session::day).distinct().count() < sessions.size()) {
            throw new PlanRuleViolationException("plan.duplicate-day");
        }
        this.id = Objects.requireNonNull(id);
        this.client = Objects.requireNonNull(client);
        this.goal = Objects.requireNonNull(goal);
        this.weeks = weeks;
        this.firstWeek = firstWeek;
        this.sessions = sessions.stream().sorted(Comparator.comparing(Session::day)).toList();
        this.version = version;
        this.automaticDraft = automaticDraft;
    }

    /** Starts a new draft, which has no version until it is stored. */
    public static Plan draft(AccountId client, Goal goal, int weeks, List<Session> sessions) {
        return new Plan(PlanId.random(), client, goal, weeks, null, sessions, 0);
    }

    /** Starts a draft built automatically, which must be complete. */
    public static Plan built(AccountId client, Goal goal, int weeks, List<Session> sessions) {
        Plan plan = new Plan(PlanId.random(), client, goal, weeks, null, sessions, 0, AutomaticDraft.BUILT);
        plan.requireComplete();
        return plan;
    }

    /** Starts the empty draft that stands for a drafting that failed. */
    public static Plan failed(AccountId client, Goal goal, int weeks) {
        return new Plan(PlanId.random(), client, goal, weeks, null, List.of(), 0, AutomaticDraft.FAILED);
    }

    /** What the system did with this draft; empty once the trainer has saved it, and for any published plan. */
    public Optional<AutomaticDraft> getAutomaticDraft() {
        return Optional.ofNullable(automaticDraft);
    }

    public Optional<LocalDate> getFirstWeek() {
        return Optional.ofNullable(firstWeek);
    }

    public boolean isPublished() {
        return firstWeek != null;
    }

    /** Refuses a plan that could not be trained yet: too few sessions, or a session without exercises. */
    public void requireComplete() {
        if (sessions.size() < MIN_SESSIONS_WHEN_COMPLETE) {
            throw new PlanRuleViolationException("plan.too-few-sessions");
        }
        if (sessions.stream().anyMatch(session -> session.entries().isEmpty())) {
            throw new PlanRuleViolationException("plan.session-without-exercises");
        }
    }

    /**
     * Returns this plan with new values, the same first week and no mark, because it is now the trainer's. A published
     * plan must stay complete, and is refused
     * only when its length gets shorter and falls below the current plan week, so a finished plan can still be saved.
     * {@code today} is the date in the client's time zone.
     */
    public Plan edit(Goal goal, int weeks, List<Session> sessions, LocalDate today) {
        Plan edited = new Plan(id, client, goal, weeks, firstWeek, sessions, version);
        if (isPublished()) {
            edited.requireComplete();
            int currentWeek = weeksSinceFirst(today);
            if (weeks < this.weeks && weeks < currentWeek) {
                throw new PlanWeeksBeforeCurrentException(currentWeek);
            }
        }
        return edited;
    }

    /**
     * Returns this draft published and without a mark, with plan week 1 starting on the Monday of the week of {@code today}, the date in
     * the client's time zone. Only a complete draft can be published, and only once.
     */
    public Plan publish(LocalDate today) {
        if (isPublished()) {
            throw new PlanAlreadyPublishedException();
        }
        requireComplete();
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return new Plan(id, client, goal, weeks, monday, sessions, version);
    }

    /**
     * The plan week {@code today} falls in, for a published plan: Monday-to-Sunday weeks from the first week, at least
     * 1. Above {@code weeks} means the plan has finished. {@code today} is the date in the client's time zone.
     */
    public Optional<Integer> currentWeek(LocalDate today) {
        return isPublished() ? Optional.of(weeksSinceFirst(today)) : Optional.empty();
    }

    // a client who moved to a time zone behind the one of publishing can, for a moment, be before week 1
    private int weeksSinceFirst(LocalDate today) {
        return Math.max(1, (int) Math.floorDiv(ChronoUnit.DAYS.between(firstWeek, today), DAYS_PER_WEEK) + 1);
    }
}
