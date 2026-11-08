package com.kinetiq.readiness.domain.model;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;

/**
 * One client's answers for one date, their local date. A client has at most one check-in a date; changing it replaces
 * the answers. Whether it may still be saved depends on the plan and the logged sets, so the service decides that.
 */
@Getter
public class CheckIn {

    private final CheckInId id;
    private final AccountId client;
    private final LocalDate date;
    private Answers answers;
    private Instant savedAt;

    /** Rebuilds a stored check-in. */
    public CheckIn(CheckInId id, AccountId client, LocalDate date, Answers answers, Instant savedAt) {
        this.id = Objects.requireNonNull(id);
        this.client = Objects.requireNonNull(client);
        this.date = Objects.requireNonNull(date);
        this.answers = Objects.requireNonNull(answers);
        this.savedAt = Objects.requireNonNull(savedAt);
    }

    /** A new check-in of the client for {@code date}. */
    public static CheckIn give(AccountId client, LocalDate date, Answers answers, Instant now) {
        return new CheckIn(CheckInId.random(), client, date, answers, now);
    }

    /** Replaces the answers; the same answers change nothing, so a repeated save gives the same result. */
    public void change(Answers answers, Instant now) {
        if (this.answers.equals(Objects.requireNonNull(answers))) {
            return;
        }
        this.answers = answers;
        this.savedAt = now;
    }

    public Readiness readiness() {
        return Readiness.of(answers);
    }
}
