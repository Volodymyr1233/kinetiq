package com.kinetiq.sessionlog.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.time.LocalDate;
import java.util.Optional;

public interface SessionLogRepository {

    /** Finds the session the client started on {@code date}. */
    Optional<SessionLog> findByClientAndDate(AccountId client, LocalDate date);

    /** Finds the client's own session; another client's session is not found. */
    Optional<SessionLog> findByIdAndClient(SessionLogId id, AccountId client);

    /** The session a set id is used in, whoever's it is, so an id of another session is never taken over. */
    Optional<SessionLogId> sessionOfSetLog(SetLogId id);

    /** Inserts the session log or replaces the stored one with its set logs, and returns it as stored. */
    SessionLog save(SessionLog log);
}
