package com.kinetiq.planchange.domain.model;

import java.time.DayOfWeek;
import java.util.Objects;

/** An entry of the removed session that goes to the end of the session on {@code toDay}. */
public record SessionMove(EntryRef from, DayOfWeek toDay) {

    public SessionMove {
        Objects.requireNonNull(from);
        Objects.requireNonNull(toDay);
    }
}
