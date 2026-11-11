package com.kinetiq.planchange.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.time.Instant;
import java.util.List;

/** What the Approvals list of a trainer holds of plan changes, read over their clients' rows. */
public interface PlanChangeApprovals {

    /** One plan change with the name of its client. */
    record Entry(PlanChangeId id, String clientName) {}

    /**
     * The plan changes of the trainer's own clients that have not ended or that ended at or after {@code endedSince},
     * the one that changed last first. Nobody else's client is ever in it.
     */
    List<Entry> forTrainer(AccountId trainer, Instant endedSince);
}
