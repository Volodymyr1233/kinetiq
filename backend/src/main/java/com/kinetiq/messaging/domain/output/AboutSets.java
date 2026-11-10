package com.kinetiq.messaging.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.AboutSet;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.util.Optional;

/** The logged sets a message can be about. */
public interface AboutSets {

    /** Returns the set log when it belongs to a session log of {@code sender}; empty for anyone else's and an unknown one. */
    Optional<AboutSet> ofSender(AccountId sender, SetLogId setLog);
}
