package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.account.domain.model.AccountId;
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
}
