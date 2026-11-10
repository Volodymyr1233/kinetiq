package com.kinetiq.planchange.domain.model;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Objects;

/** One session of a plan, with its entries in order; the position of an entry is its index. */
public record SessionSnapshot(DayOfWeek day, String name, List<EntrySnapshot> entries) {

    public SessionSnapshot {
        Objects.requireNonNull(day);
        Objects.requireNonNull(name);
        entries = List.copyOf(entries);
    }
}
