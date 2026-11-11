package com.kinetiq.planchange.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.util.List;

/** What a trainer reads of the plan changes of their own clients, with title and reason in the trainer's language. */
public interface ViewPlanChangeUseCase {

    /** One plan change of the trainer's own client; {@code PlanChangeNotFoundException} for any other. */
    PlanChangeView get(AccountId trainer, PlanChangeId id);

    /**
     * The open plan change of the trainer's own client. {@code ClientNotFoundException} for another trainer's client,
     * {@code PlanChangeNotFoundException} when the client has none open.
     */
    PlanChangeSummary openOf(AccountId trainer, AccountId client);

    /**
     * The plan changes of the trainer's own clients that have not ended or ended in the last 30 days, the one that
     * changed last first.
     */
    List<PlanChangeApprovalView> approvals(AccountId trainer);

    /**
     * The last 5 plan changes of the trainer's own client that ended, the latest first. {@code ClientNotFoundException}
     * for another trainer's client.
     */
    List<PlanChangeApprovalView> ended(AccountId trainer, AccountId client);
}
