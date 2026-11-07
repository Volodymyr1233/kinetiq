package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.ListDraftedPlansUseCase;
import com.kinetiq.plan.domain.model.DraftedPlan;
import com.kinetiq.plan.domain.output.DraftedPlanDirectory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DraftedPlanListService implements ListDraftedPlansUseCase {

    private final DraftedPlanDirectory drafts;

    @Override
    public List<DraftedPlan> draftedPlans(AccountId trainer) {
        return drafts.findOf(trainer);
    }
}
