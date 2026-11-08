package com.kinetiq.sessionlog.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogHeader;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SessionLogRepository {

    /** Finds the session the client started on {@code date}. */
    Optional<SessionLog> findByClientAndDate(AccountId client, LocalDate date);

    /** Finds the client's own session; another client's session is not found. */
    Optional<SessionLog> findByIdAndClient(SessionLogId id, AccountId client);

    /** The session a set id is used in, whoever's it is, so an id of another session is never taken over. */
    Optional<SessionLogId> sessionOfSetLog(SetLogId id);

    /** The headers of the client's session logs dated {@code from} to {@code to}, both included. */
    List<SessionLogHeader> headersBetween(AccountId client, LocalDate from, LocalDate to);

    /** Every session log of the client with its entries and set logs, newest date first. */
    List<SessionLog> findAllByClient(AccountId client);

    /**
     * Inserts the session log or replaces the stored one with its set logs, and returns it as stored. A copy that was
     * changed by another request since it was read is {@code SessionLogChangedException}.
     */
    SessionLog save(SessionLog log);
}
