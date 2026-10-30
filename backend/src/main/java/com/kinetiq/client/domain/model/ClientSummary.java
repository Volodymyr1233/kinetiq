package com.kinetiq.client.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.Goal;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/** What the trainer's client list shows of one client. The goal is the quiz's, so it is absent before the quiz. */
@Getter
public class ClientSummary {

    private final AccountId id;
    private final String name;

    @Getter(AccessLevel.NONE)
    private final Goal goal;

    private final PlanState planState;

    public ClientSummary(AccountId id, String name, Goal goal, PlanState planState) {
        this.id = Objects.requireNonNull(id);
        this.name = Objects.requireNonNull(name);
        this.goal = goal;
        this.planState = Objects.requireNonNull(planState);
    }

    public Optional<Goal> getGoal() {
        return Optional.ofNullable(goal);
    }
}
