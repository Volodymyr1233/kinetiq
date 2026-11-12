package com.kinetiq.overview.domain.model;

/** The sessions a client did of those due in a window; there is no completion without a due session. */
public record Completion(int done, int due) {

    public Completion {
        if (due < 1 || done < 0 || done > due) {
            throw new IllegalArgumentException("done is between 0 and at least one due session");
        }
    }

    /** Whether fewer than {@code percent} of the due sessions were done, decided without rounding. */
    public boolean below(int percent) {
        return done * 100 < percent * due;
    }
}
