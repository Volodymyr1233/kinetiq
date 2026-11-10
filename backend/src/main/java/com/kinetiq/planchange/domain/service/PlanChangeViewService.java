package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.PlanChangeSummary;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import com.kinetiq.planchange.domain.model.SessionMove;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlanChangeViewService implements ViewPlanChangeUseCase {

    private final ViewClientsUseCase clients;
    private final CurrentAccountUseCase accounts;
    private final PlanChangeRepository planChanges;
    private final PlanSource plans;

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
        Language language = language(trainer);
        Map<ExerciseId, ExerciseLabel> exercises = plans.exercises(exercisesOf(planChange), language);
        return new PlanChangeView(planChange, client.getName(), text(language, planChange, exercises), exercises);
    }

    @Override
    public PlanChangeSummary openOf(AccountId trainer, AccountId client) {
        clients.getClient(trainer, client);
        PlanChange planChange = planChanges.findOpen(client).orElseThrow(PlanChangeNotFoundException::new);
        Language language = language(trainer);
        return new PlanChangeSummary(
                planChange, text(language, planChange, plans.exercises(exercisesOf(planChange), language)));
    }

    private Language language(AccountId trainer) {
        return accounts.find(trainer).getSettings().language();
    }

    private static PlanChangeText text(
            Language language, PlanChange planChange, Map<ExerciseId, ExerciseLabel> exercises) {
        Map<ExerciseId, String> names = exercises.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().name()));
        return PlanChangeWording.of(language, planChange, names);
    }

    private static Set<ExerciseId> exercisesOf(PlanChange planChange) {
        PlanChangeContent content = planChange.getContent();
        Set<ExerciseId> exercises = new HashSet<>(planChange.getConcernedExercises());
        content.changes().stream().map(TargetChange::exercise).forEach(exercises::add);
        content.moves().stream().map(SessionMove::exercise).forEach(exercises::add);
        return exercises;
    }
}
