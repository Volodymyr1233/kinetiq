package com.kinetiq.plan.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftedPlan;
import java.util.List;

public interface ListDraftedPlansUseCase {

    /**
     * The trainer's own clients whose plan is still a draft, the one waiting longest first. A client whose first plan
     * is still being drafted has no plan yet, so is absent.
     */
    List<DraftedPlan> draftedPlans(AccountId trainer);
}
