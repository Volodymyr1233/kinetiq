package com.kinetiq.sessionlog.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.sessionlog.domain.exception.NoteInvalidException;
import com.kinetiq.sessionlog.domain.exception.NotFinishedException;
import com.kinetiq.sessionlog.domain.exception.NothingLoggedException;
import com.kinetiq.sessionlog.domain.exception.SessionLogLockedException;
import com.kinetiq.sessionlog.domain.exception.SetLogConflictException;
import com.kinetiq.sessionlog.domain.exception.SetValuesInvalidException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * One session a client started on one date: a copy of the planned session, and the sets logged in it. The copy is
 * taken at the start, because a plan save replaces the plan's sessions.
 */
@Getter
public class SessionLog {

    public static final int NOTE_MAX_LENGTH = 500;

    private static final int RPE_DECIMALS = 1;

    // also trims no-break and other Unicode spaces, as a session name does
    private static final Pattern EDGE_SPACES = Pattern.compile("^[\\s\\p{Z}]+|[\\s\\p{Z}]+$");

    private final SessionLogId id;
    private final AccountId client;
    private final LocalDate date;
    private final String name;
    private final Instant startedAt;

    @Getter(AccessLevel.NONE)
    private Instant finishedAt;

    @Getter(AccessLevel.NONE)
    private String note;

    private final List<LoggedEntry> entries;
    private final List<SetLog> setLogs;
    private final long version;

    /** Rebuilds a stored session log without a note. */
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
        this(id, client, date, name, startedAt, finishedAt, null, entries, setLogs, version);
    }

    /** Rebuilds a stored session log; {@code finishedAt} and {@code note} are {@code null} until they are set. */
    public SessionLog(
            SessionLogId id,
            AccountId client,
            LocalDate date,
            String name,
            Instant startedAt,
            Instant finishedAt,
            String note,
            List<LoggedEntry> entries,
            List<SetLog> setLogs,
            long version) {
        this.id = Objects.requireNonNull(id);
        this.client = Objects.requireNonNull(client);
        this.date = Objects.requireNonNull(date);
        this.name = Objects.requireNonNull(name);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.finishedAt = finishedAt;
        this.note = note;
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

    /** What the client wrote for their trainer; empty until they write one. */
    public Optional<String> getNote() {
        return Optional.ofNullable(note);
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

    /**
     * Finishes the session. A repeat changes nothing, also after the lock, so a send that is repeated is safe. Otherwise
     * the session must still be open ({@code SessionLogLockedException}) and have a logged set
     * ({@code NothingLoggedException}).
     */
    public void finish(Instant now) {
        if (finishedAt != null) {
            return;
        }
        requireOpen(now);
        if (setLogs.isEmpty()) {
            throw new NothingLoggedException();
        }
        finishedAt = now;
    }

    /**
     * Writes the note for the trainer, replacing the one before; empty text removes it. Only a finished session has a
     * note ({@code NotFinishedException}), and only while it is open ({@code SessionLogLockedException}). The text is
     * trimmed, and one over {@value #NOTE_MAX_LENGTH} characters is {@code NoteInvalidException}.
     */
    public void writeNote(String text, Instant now) {
        if (finishedAt == null) {
            throw new NotFinishedException();
        }
        requireOpen(now);
        String trimmed = EDGE_SPACES.matcher(text).replaceAll("");
        if (trimmed.length() > NOTE_MAX_LENGTH) {
            throw new NoteInvalidException();
        }
        note = trimmed.isEmpty() ? null : trimmed;
    }

    /** The summary of this finished session, as its sets are now; {@code measures} gives each exercise its measure. */
    public SessionSummary summary(Map<ExerciseId, Measure> measures) {
        return SessionSummary.of(this, measures);
    }

    /** How many sets the session planned, over all its entries. */
    public int setsPlanned() {
        return entries.stream().mapToInt(LoggedEntry::sets).sum();
    }

    /** The whole minutes from the start to the finish, at least 1; empty until the session is finished. */
    public Optional<Integer> minutes() {
        return getFinishedAt()
                .map(finished -> (int) Math.max(1, Duration.between(startedAt, finished).toMinutes()));
    }

    /** The mean RPE of the logged sets to one decimal, rounded half up; empty while no set is logged. */
    public Optional<BigDecimal> averageRpe() {
        if (setLogs.isEmpty()) {
            return Optional.empty();
        }
        int sum = setLogs.stream().mapToInt(set -> set.values().rpe()).sum();
        return Optional.of(
                BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(setLogs.size()), RPE_DECIMALS, RoundingMode.HALF_UP));
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
        if (!EditWindow.isOpen(startedAt, now)) {
            throw new SessionLogLockedException();
        }
    }
}
