package com.kinetiq.plan.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftedPlan;
import java.util.List;

public interface DraftedPlanDirectory {

    /** The unpublished plans of the trainer's own clients, oldest first, then by plan id. */
    List<DraftedPlan> findOf(AccountId trainer);
}
