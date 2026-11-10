package com.kinetiq.planchange.domain.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A client's published plan as a plan change sees it: its version, first Monday, length in weeks and sessions in week
 * order.
 */
public record PlanSnapshot(long version, LocalDate firstWeek, int weeks, List<SessionSnapshot> sessions) {

    public PlanSnapshot {
        Objects.requireNonNull(firstWeek);
        sessions = sessions.stream().sorted(Comparator.comparing(SessionSnapshot::day)).toList();
    }

    /** The Monday of the plan's last week. */
    public LocalDate lastWeek() {
        return firstWeek.plusWeeks(weeks - 1L);
    }

    public Optional<SessionSnapshot> session(DayOfWeek day) {
        return sessions.stream().filter(session -> session.day() == day).findFirst();
    }

    public Optional<EntrySnapshot> entry(EntryRef ref) {
        return session(ref.day())
                .filter(session -> ref.position() < session.entries().size())
                .map(session -> session.entries().get(ref.position()));
    }

    /**
     * The plan as it would be with the content: changed targets replaced, the removed session gone and moved entries
     * appended to their sessions in the order of the moves. Every entry and session the content names must exist.
     */
    public PlanSnapshot apply(PlanChangeContent content) {
        List<SessionSnapshot> result = new ArrayList<>();
        for (SessionSnapshot session : sessions) {
            if (content.removed().map(removed -> removed.day() == session.day()).orElse(false)) {
                continue;
            }
            List<EntrySnapshot> entries = new ArrayList<>();
            for (int position = 0; position < session.entries().size(); position++) {
                EntryRef ref = new EntryRef(session.day(), position);
                EntrySnapshot entry = session.entries().get(position);
                for (TargetChange change : content.changes()) {
                    if (change.entry().equals(ref)) {
                        entry = entry.with(change.target(), change.newValue());
                    }
                }
                entries.add(entry);
            }
            content.moves().stream()
                    .filter(move -> move.toDay() == session.day())
                    .forEach(move -> entries.add(entry(move.from()).orElseThrow()));
            result.add(new SessionSnapshot(session.day(), session.name(), entries));
        }
        return new PlanSnapshot(version, firstWeek, weeks, result);
    }
}
