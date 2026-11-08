package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import java.util.List;

public interface ViewClientTrainingUseCase {

    /**
     * The current plan week, the four most recent sessions and the quiz answers of one of the trainer's own clients, by
     * the client's local date. Another trainer's client and an unknown one are both {@code ClientNotFoundException}.
     */
    ClientOverview overview(AccountId trainer, AccountId client);

    /**
     * Every session of one of the trainer's own clients, newest first: each session log, and each planned day from the
     * publish day until yesterday that was never started. Another trainer's client and an unknown one are both
     * {@code ClientNotFoundException}.
     */
    List<ClientSession> sessions(AccountId trainer, AccountId client);

    /**
     * One session log of one of the trainer's own clients, with its sets. Another trainer's client and an unknown one
     * are both {@code ClientNotFoundException}; a log that is not this client's, or does not exist, is
     * {@code SessionLogNotFoundException}.
     */
    ClientSessionLog sessionLog(AccountId trainer, AccountId client, SessionLogId id);
}
