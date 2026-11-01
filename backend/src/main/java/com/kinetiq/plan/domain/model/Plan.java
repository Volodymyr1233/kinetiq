package com.kinetiq.plan.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * The training prescribed for one client. A plan with a first week is published; the first week is the Monday of
 * plan week 1. Sessions are kept in week order, Monday first.
 */
@Getter
public class Plan {

    private final PlanId id;
    private final AccountId client;
    private final Goal goal;
    private final int weeks;

    @Getter(AccessLevel.NONE)
    private final LocalDate firstWeek;

    private final List<Session> sessions;
    private final long version;

    /** Rebuilds a stored plan; {@code firstWeek} is {@code null} for a draft. */
    public Plan(
            PlanId id,
            AccountId client,
            Goal goal,
            int weeks,
            LocalDate firstWeek,
            List<Session> sessions,
            long version) {
        this.id = Objects.requireNonNull(id);
        this.client = Objects.requireNonNull(client);
        this.goal = Objects.requireNonNull(goal);
        this.weeks = weeks;
        this.firstWeek = firstWeek;
        this.sessions = sessions.stream().sorted(Comparator.comparing(Session::day)).toList();
        this.version = version;
    }

    /** Starts a new draft, which has no version until it is stored. */
    public static Plan draft(AccountId client, Goal goal, int weeks, List<Session> sessions) {
        return new Plan(PlanId.random(), client, goal, weeks, null, sessions, 0);
    }

    public Optional<LocalDate> getFirstWeek() {
        return Optional.ofNullable(firstWeek);
    }

    public boolean isPublished() {
        return firstWeek != null;
    }
}
