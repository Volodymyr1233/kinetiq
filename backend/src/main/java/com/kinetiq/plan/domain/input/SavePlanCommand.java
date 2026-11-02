package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import java.util.List;
import java.util.Objects;

/** A plan as the trainer's editor holds it. {@code version} is {@code null} on a first save. */
public record SavePlanCommand(
        AccountId trainer, AccountId client, Long version, Goal goal, int weeks, List<Session> sessions) {

    public SavePlanCommand {
        Objects.requireNonNull(trainer);
        Objects.requireNonNull(client);
        Objects.requireNonNull(goal);
        sessions = List.copyOf(sessions);
    }
}
