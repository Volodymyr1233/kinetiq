package com.kinetiq.planchange.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The proposed changes of a plan change: changed targets, a removed session and the entries that move out of it. It
 * always changes something; moves come only from a removed session, at most {@value #MAX_MOVES}.
 */
public record PlanChangeContent(List<TargetChange> changes, Optional<RemovedSession> removed, List<SessionMove> moves) {

    public static final int MAX_MOVES = 2;

    public PlanChangeContent {
        changes = List.copyOf(changes);
        Objects.requireNonNull(removed);
        moves = List.copyOf(moves);
        if (changes.isEmpty() && removed.isEmpty()) {
            throw new IllegalArgumentException("content must change something");
        }
        if (moves.size() > MAX_MOVES) {
            throw new IllegalArgumentException("too many moves");
        }
        if (moves.stream()
                .anyMatch(move -> removed.isEmpty() || move.from().day() != removed.get().day())) {
            throw new IllegalArgumentException("a move must come from the removed session");
        }
    }

    /** Content that only changes targets. */
    public static PlanChangeContent ofChanges(List<TargetChange> changes) {
        return new PlanChangeContent(changes, Optional.empty(), List.of());
    }
}
