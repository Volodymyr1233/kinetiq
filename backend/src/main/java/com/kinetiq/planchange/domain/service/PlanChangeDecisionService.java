package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.DecidePlanChangeUseCase;
import com.kinetiq.planchange.domain.input.KeepCurrentPlanCommand;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import java.time.Clock;

/** The decisions of a trainer on the plan changes of their own clients. */
public class PlanChangeDecisionService implements DecidePlanChangeUseCase {

    private final ViewClientsUseCase clients;
    private final PlanChangeRepository planChanges;
    private final PlanChangeTexts texts;
    private final PlanChangeDecider decider;

    public PlanChangeDecisionService(
            ViewClientsUseCase clients,
            CurrentAccountUseCase accounts,
            PlanChangeRepository planChanges,
            PlanSource plans,
            ExchangeMessagesUseCase messages,
            Clock clock) {
        this.clients = clients;
        this.planChanges = planChanges;
        this.texts = new PlanChangeTexts(plans, accounts);
        this.decider = new PlanChangeDecider(planChanges, texts, messages, clock);
    }

    @Override
    public PlanChangeView accept(AccountId trainer, PlanChangeId id) {
        ClientSummary client = ownClient(trainer, id);
        return view(trainer, client, decider.accept(id, Side.TRAINER, client.getTimeZone()));
    }

    @Override
    public PlanChangeView undo(AccountId trainer, PlanChangeId id) {
        ClientSummary client = ownClient(trainer, id);
        return view(trainer, client, decider.undo(id, Side.TRAINER, client.getTimeZone()));
    }

    @Override
    public PlanChangeView keepCurrent(KeepCurrentPlanCommand command) {
        ClientSummary client = ownClient(command.person(), command.id());
        return view(command.person(), client, decider.keepCurrent(command, Side.TRAINER, client.getTimeZone()));
    }

    // another trainer's plan change is not found either, so it cannot be told apart from a missing one
    private ClientSummary ownClient(AccountId trainer, PlanChangeId id) {
        PlanChange planChange = planChanges.find(id).orElseThrow(PlanChangeNotFoundException::new);
        try {
            return clients.getClient(trainer, planChange.getClient());
        } catch (ClientNotFoundException notOwn) {
            throw new PlanChangeNotFoundException();
        }
    }

    private PlanChangeView view(AccountId trainer, ClientSummary client, PlanChange planChange) {
        return texts.trainerView(planChange, client.getName(), texts.languageOf(trainer));
    }
}
