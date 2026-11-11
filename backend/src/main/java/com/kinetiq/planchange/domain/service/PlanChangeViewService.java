package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.PlanChangeApprovalView;
import com.kinetiq.planchange.domain.input.PlanChangeSummary;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeApprovals;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlanChangeViewService implements ViewPlanChangeUseCase {

    /** How long a plan change that ended stays in the Approvals list. */
    static final Duration DECIDED_WINDOW = Duration.ofDays(30);

    /** How many ended plan changes the page of a client shows. */
    static final int ENDED_SHOWN = 5;

    private final ViewClientsUseCase clients;
    private final PlanChangeRepository planChanges;
    private final PlanChangeApprovals approvals;
    private final Clock clock;
    private final PlanChangeTexts texts;

    public PlanChangeViewService(
            ViewClientsUseCase clients,
            CurrentAccountUseCase accounts,
            PlanChangeRepository planChanges,
            PlanSource plans,
            PlanChangeApprovals approvals,
            Clock clock) {
        this.clients = clients;
        this.planChanges = planChanges;
        this.approvals = approvals;
        this.clock = clock;
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

    @Override
    public List<PlanChangeApprovalView> approvals(AccountId trainer) {
        List<PlanChangeApprovals.Entry> entries = approvals.forTrainer(trainer, clock.instant().minus(DECIDED_WINDOW));
        Map<PlanChangeId, PlanChange> found = planChanges.findAll(entries.stream().map(PlanChangeApprovals.Entry::id).toList()).stream()
                .collect(Collectors.toMap(PlanChange::getId, Function.identity()));
        Language language = texts.languageOf(trainer);
        return entries.stream()
                // a plan change gone between the two reads has nothing to show
                .filter(entry -> found.containsKey(entry.id()))
                .map(entry -> new PlanChangeApprovalView(
                        found.get(entry.id()), entry.clientName(), texts.text(language, found.get(entry.id()))))
                .toList();
    }

    @Override
    public List<PlanChangeApprovalView> ended(AccountId trainer, AccountId client) {
        String name = clients.getClient(trainer, client).getName();
        Language language = texts.languageOf(trainer);
        return planChanges.findEnded(client, ENDED_SHOWN).stream()
                .map(planChange -> new PlanChangeApprovalView(planChange, name, texts.text(language, planChange)))
                .toList();
    }
}
