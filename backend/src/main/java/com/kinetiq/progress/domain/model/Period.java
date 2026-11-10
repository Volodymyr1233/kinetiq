package com.kinetiq.progress.domain.model;

import com.kinetiq.progress.domain.exception.PeriodInvalidException;
import java.util.Set;

/** The plan weeks a client's progress covers, ending with the current one: 4, 8 or 12. */
public record Period(int weeks) {

    private static final Set<Integer> ALLOWED = Set.of(4, 8, 12);

    public Period {
        if (!ALLOWED.contains(weeks)) {
            throw new PeriodInvalidException();
        }
    }
}
