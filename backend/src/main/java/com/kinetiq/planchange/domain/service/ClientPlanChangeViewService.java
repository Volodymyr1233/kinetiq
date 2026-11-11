package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.input.ViewOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;

public class ClientPlanChangeViewService implements ViewOwnPlanChangeUseCase {

    private final PlanChangeRepository planChanges;
    private final PlanChangeTexts texts;

    public ClientPlanChangeViewService(
            CurrentAccountUseCase accounts, PlanChangeRepository planChanges, PlanSource plans) {
        this.planChanges = planChanges;
        this.texts = new PlanChangeTexts(plans, accounts);
    }

    @Override
    public ClientPlanChangeView current(AccountId client) {
        return texts.clientView(planChanges.findOpen(client).orElseThrow(PlanChangeNotFoundException::new));
    }

    @Override
    public ClientPlanChangeView get(AccountId client, PlanChangeId id) {
        // another client's plan change is not found either, so it cannot be told apart from a missing one
        PlanChange planChange = planChanges
                .find(id)
                .filter(found -> found.getClient().equals(client))
                .orElseThrow(PlanChangeNotFoundException::new);
        return texts.clientView(planChange);
    }
}
