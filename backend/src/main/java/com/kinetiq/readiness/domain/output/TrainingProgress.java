package com.kinetiq.readiness.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import java.time.LocalDate;

/**
 * What the client has trained so far. It is implemented by the feature that logs sets, so that feature may depend on
 * readiness and readiness never depends on it.
 */
public interface TrainingProgress {

    /** Whether at least one set of the client's session on {@code date} is logged. */
    boolean hasLoggedSet(AccountId client, LocalDate date);
}
