package com.kinetiq.messaging.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.Goal;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * One row of a person's conversation list. {@code name} is the other person's. The goal and the plan week are
 * for a trainer's row, each absent until it exists; the last message is absent while nobody has written.
 */
@Getter
public class ConversationOverview {

    private final AccountId client;
    private final String name;

    @Getter(AccessLevel.NONE)
    private final Goal goal;

    @Getter(AccessLevel.NONE)
    private final PlanWeek planWeek;

    private final int unread;

    @Getter(AccessLevel.NONE)
    private final LastMessage lastMessage;

    public ConversationOverview(
            AccountId client, String name, Goal goal, PlanWeek planWeek, int unread, LastMessage lastMessage) {
        this.client = Objects.requireNonNull(client);
        this.name = Objects.requireNonNull(name);
        this.goal = goal;
        this.planWeek = planWeek;
        this.unread = unread;
        this.lastMessage = lastMessage;
    }

    public Optional<Goal> getGoal() {
        return Optional.ofNullable(goal);
    }

    public Optional<PlanWeek> getPlanWeek() {
        return Optional.ofNullable(planWeek);
    }

    public Optional<LastMessage> getLastMessage() {
        return Optional.ofNullable(lastMessage);
    }
}
