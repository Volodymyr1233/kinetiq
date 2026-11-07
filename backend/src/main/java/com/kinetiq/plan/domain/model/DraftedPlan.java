package com.kinetiq.plan.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.util.Objects;

/**
 * A client's plan while it is still a draft, as the trainer's approvals list shows it. {@code createdAt} is when the
 * plan was first stored, so a rebuild or a save does not change its place in the list.
 */
public record DraftedPlan(
        AccountId client, String clientName, Kind kind, int weeks, int sessionsPerWeek, Instant createdAt) {

    /** How the draft came to be: built or failed by the system, or saved by the trainer. */
    public enum Kind {
        BUILT,
        FAILED,
        SAVED
    }

    public DraftedPlan {
        Objects.requireNonNull(client);
        Objects.requireNonNull(clientName);
        Objects.requireNonNull(kind);
        Objects.requireNonNull(createdAt);
    }
}
