package com.kinetiq.planchange.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An answer of the drafting service as it came, keyed by the keys of its prompt: nothing in it is checked yet. A value
 * left out of a change is {@code null}.
 */
public record ProposedContent(List<Change> changes, Optional<String> removeSession, List<Move> moves) {

    public ProposedContent {
        changes = List.copyOf(changes);
        Objects.requireNonNull(removeSession);
        moves = List.copyOf(moves);
    }

    /** New values for the entry with key {@code entry}, such as {@code s1e2}. */
    public record Change(String entry, Integer sets, Integer amount, BigDecimal loadKg) {}

    /** The entry with key {@code entry} goes to the end of the session with key {@code toSession}. */
    public record Move(String entry, String toSession) {}
}
