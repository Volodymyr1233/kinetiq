package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLog;

public interface LogSessionUseCase {

    /**
     * Starts the session the client's plan has today, or returns the one started already today. A draft and a missing
     * plan are {@code PlanNotFoundException}; a rest day and a finished plan are {@code NoSessionTodayException}.
     */
    StartedSession start(AccountId client);

    /**
     * Logs a set into the client's own session, or changes it when the id was logged already. A session that is not the
     * client's is {@code SessionLogNotFoundException}, and so is a set id that belongs to another session. From 24 hours
     * after the session started, a new set or a change is {@code SessionLogLockedException}.
     */
    SetLog logSet(LogSetCommand command);

    /**
     * Finishes the client's own session, or answers as the first time when it was finished already. A session that is not
     * the client's is {@code SessionLogNotFoundException}. An unfinished one from 24 hours after the start is
     * {@code SessionLogLockedException}, and one with no logged set is {@code NothingLoggedException}.
     */
    SessionSummaryView finish(AccountId client, SessionLogId id);

    /**
     * The summary of the client's own finished session, as its sets are now. A session that is not the client's is
     * {@code SessionLogNotFoundException}, and one not finished is {@code NotFinishedException}.
     */
    SessionSummaryView summary(AccountId client, SessionLogId id);
}
