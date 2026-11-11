package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.PlanChangeSummary;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;

public class PlanChangeViewService implements ViewPlanChangeUseCase {

    private final ViewClientsUseCase clients;
    private final PlanChangeRepository planChanges;
    private final PlanChangeTexts texts;

    public PlanChangeViewService(
            ViewClientsUseCase clients,
            CurrentAccountUseCase accounts,
            PlanChangeRepository planChanges,
            PlanSource plans) {
        this.clients = clients;
        this.planChanges = planChanges;
        this.texts = new PlanChangeTexts(plans, accounts);
    }

    @Override
    public PlanChangeView get(AccountId trainer, PlanChangeId id) {
        PlanChange planChange = planChanges.find(id).orElseThrow(PlanChangeNotFoundException::new);
        ClientSummary client;
        try {
            client = clients.getClient(trainer, planChange.getClient());
        } catch (ClientNotFoundException notOwn) {
            // another trainer's plan change is not found either, so it cannot be told apart from a missing one
            throw new PlanChangeNotFoundException();
        }
        return texts.trainerView(planChange, client.getName(), texts.languageOf(trainer));
    }

    @Override
    public PlanChangeSummary openOf(AccountId trainer, AccountId client) {
        clients.getClient(trainer, client);
        PlanChange planChange = planChanges.findOpen(client).orElseThrow(PlanChangeNotFoundException::new);
        return new PlanChangeSummary(planChange, texts.text(texts.languageOf(trainer), planChange));
    }
}
