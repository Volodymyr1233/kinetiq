package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.input.DecideOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.input.KeepCurrentPlanCommand;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import java.time.Clock;
import java.time.ZoneId;

/** The decisions of a client on their own plan changes. */
public class OwnPlanChangeDecisionService implements DecideOwnPlanChangeUseCase {

    private final CurrentAccountUseCase accounts;
    private final PlanChangeRepository planChanges;
    private final PlanChangeTexts texts;
    private final PlanChangeDecider decider;

    public OwnPlanChangeDecisionService(
            CurrentAccountUseCase accounts,
            PlanChangeRepository planChanges,
            PlanSource plans,
            ExchangeMessagesUseCase messages,
            Clock clock) {
        this.accounts = accounts;
        this.planChanges = planChanges;
        this.texts = new PlanChangeTexts(plans, accounts);
        this.decider = new PlanChangeDecider(planChanges, texts, messages, clock);
    }

    @Override
    public ClientPlanChangeView accept(AccountId client, PlanChangeId id) {
        requireOwn(client, id);
        ZoneId zone = accounts.find(client).getTimeZone();
        return texts.clientView(decider.accept(id, Side.CLIENT, zone));
    }

    @Override
    public ClientPlanChangeView undo(AccountId client, PlanChangeId id) {
        requireOwn(client, id);
        return texts.clientView(decider.undo(id, Side.CLIENT));
    }

    @Override
    public ClientPlanChangeView keepCurrent(KeepCurrentPlanCommand command) {
        requireOwn(command.person(), command.id());
        ZoneId zone = accounts.find(command.person()).getTimeZone();
        return texts.clientView(decider.keepCurrent(command, Side.CLIENT, zone));
    }

    // another client's plan change is not found either, so it cannot be told apart from a missing one
    private void requireOwn(AccountId client, PlanChangeId id) {
        planChanges
                .find(id)
                .filter(found -> found.getClient().equals(client))
                .orElseThrow(PlanChangeNotFoundException::new);
    }
}
