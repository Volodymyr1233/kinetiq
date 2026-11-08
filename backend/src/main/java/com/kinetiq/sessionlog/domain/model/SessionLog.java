package com.kinetiq.sessionlog.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.exception.SessionLogLockedException;
import com.kinetiq.sessionlog.domain.exception.SetLogConflictException;
import com.kinetiq.sessionlog.domain.exception.SetValuesInvalidException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * One session a client started on one date: a copy of the planned session, and the sets logged in it. The copy is
 * taken at the start, because a plan save replaces the plan's sessions.
 */
@Getter
public class SessionLog {

    // counted from the start, whether or not the session was finished
    private static final Duration EDIT_WINDOW = Duration.ofHours(24);

    private final SessionLogId id;
    private final AccountId client;
    private final LocalDate date;
    private final String name;
    private final Instant startedAt;

    @Getter(AccessLevel.NONE)
    private final Instant finishedAt;

    private final List<LoggedEntry> entries;
    private final List<SetLog> setLogs;
    private final long version;

    /** Rebuilds a stored session log; {@code finishedAt} is {@code null} until the session is finished. */
    public SessionLog(
            SessionLogId id,
            AccountId client,
            LocalDate date,
            String name,
            Instant startedAt,
            Instant finishedAt,
            List<LoggedEntry> entries,
            List<SetLog> setLogs,
            long version) {
        this.id = Objects.requireNonNull(id);
        this.client = Objects.requireNonNull(client);
        this.date = Objects.requireNonNull(date);
        this.name = Objects.requireNonNull(name);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.finishedAt = finishedAt;
        this.entries = List.copyOf(entries);
        this.setLogs = new ArrayList<>(setLogs);
        this.version = version;
    }

    /** Starts a session from a copy of the planned one; {@code date} is the client's local date. */
    public static SessionLog start(
            AccountId client, LocalDate date, String name, List<LoggedEntry> entries, Instant now) {
        return new SessionLog(SessionLogId.random(), client, date, name, now, null, entries, List.of(), 0);
    }

    public Optional<Instant> getFinishedAt() {
        return Optional.ofNullable(finishedAt);
    }

    public List<SetLog> getSetLogs() {
        return List.copyOf(setLogs);
    }

    /**
     * Logs a set, or changes it. The same id with the same values changes nothing, so a send that is repeated stores one
     * set, even once the session is locked. The place must be a planned set of the session; an id never moves to another
     * place, and a place is logged under one id only. From 24 hours after the start a new set or a change is
     * {@code SessionLogLockedException}.
     */
    public SetLog logSet(SetLogId setLogId, int entryPosition, int setNumber, SetValues values, Instant now) {
        LoggedEntry entry = plannedEntry(entryPosition, setNumber);
        Optional<SetLog> known = setLogs.stream().filter(s -> s.id().equals(setLogId)).findFirst();
        if (known.isPresent()) {
            return change(known.get(), entryPosition, setNumber, values, now);
        }
        requireOpen(now);
        if (setLogs.stream().anyMatch(s -> s.entryPosition() == entryPosition && s.setNumber() == setNumber)) {
            throw SetLogConflictException.placeTaken();
        }
        SetLog logged = new SetLog(
                setLogId, entryPosition, setNumber, values, entry.loadKg(), entry.amount(), entry.rpe(), now);
        setLogs.add(logged);
        return logged;
    }

    private LoggedEntry plannedEntry(int entryPosition, int setNumber) {
        if (entryPosition < 0 || entryPosition >= entries.size()) {
            throw new SetValuesInvalidException("place");
        }
        LoggedEntry entry = entries.get(entryPosition);
        if (setNumber < 1 || setNumber > entry.sets()) {
            throw new SetValuesInvalidException("place");
        }
        return entry;
    }

    private SetLog change(SetLog stored, int entryPosition, int setNumber, SetValues values, Instant now) {
        if (stored.entryPosition() != entryPosition || stored.setNumber() != setNumber) {
            throw SetLogConflictException.idInUse();
        }
        if (stored.values().equals(values)) {
            return stored;
        }
        requireOpen(now);
        SetLog changed = stored.withValues(values, now);
        setLogs.set(setLogs.indexOf(stored), changed);
        return changed;
    }

    private void requireOpen(Instant now) {
        if (!now.isBefore(startedAt.plus(EDIT_WINDOW))) {
            throw new SessionLogLockedException();
        }
    }
}
