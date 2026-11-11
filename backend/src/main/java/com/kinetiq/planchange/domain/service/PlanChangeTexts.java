package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import com.kinetiq.planchange.domain.model.SessionMove;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.planchange.domain.output.PlanSource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

/** Builds what a person reads of a plan change, in their language: the one place that knows how. */
@RequiredArgsConstructor
class PlanChangeTexts {

    private final PlanSource plans;
    private final CurrentAccountUseCase accounts;

    Language languageOf(AccountId person) {
        return accounts.find(person).getSettings().language();
    }

    PlanChangeView trainerView(PlanChange planChange, String clientName, Language language) {
        Map<ExerciseId, ExerciseLabel> exercises = plans.exercises(exercisesOf(planChange), language);
        return new PlanChangeView(planChange, clientName, text(language, planChange, exercises), exercises);
    }

    ClientPlanChangeView clientView(PlanChange planChange) {
        Account client = accounts.find(planChange.getClient());
        Language language = client.getSettings().language();
        AccountId trainer = client.getTrainerId().orElseThrow(() -> new IllegalStateException("a client has a trainer"));
        Map<ExerciseId, ExerciseLabel> exercises = plans.exercises(exercisesOf(planChange), language);
        return new ClientPlanChangeView(
                planChange, accounts.find(trainer).getName(), text(language, planChange, exercises), exercises);
    }

    PlanChangeText text(Language language, PlanChange planChange) {
        return text(language, planChange, plans.exercises(exercisesOf(planChange), language));
    }

    private PlanChangeText text(Language language, PlanChange planChange, Map<ExerciseId, ExerciseLabel> exercises) {
        Map<ExerciseId, String> names = exercises.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().name()));
        return PlanChangeWording.of(language, planChange, names, volumes(planChange));
    }

    // only the plan the content was made from tells the sets of an entry whose amount alone changed
    private Map<EntryRef, Long> volumes(PlanChange planChange) {
        return plans.currentPlan(planChange.getClient())
                .filter(plan -> plan.version() == planChange.getPlanVersion())
                .map(plan -> planChange.getContent().changes().stream()
                        .map(TargetChange::entry)
                        .distinct()
                        .filter(entry -> plan.entry(entry).isPresent())
                        .collect(Collectors.toMap(entry -> entry, entry -> plan.entry(entry).orElseThrow().volume())))
                .orElse(Map.of());
    }

    static Set<ExerciseId> exercisesOf(PlanChange planChange) {
        PlanChangeContent content = planChange.getContent();
        Set<ExerciseId> exercises = new HashSet<>(planChange.getConcernedExercises());
        content.changes().stream().map(TargetChange::exercise).forEach(exercises::add);
        content.moves().stream().map(SessionMove::exercise).forEach(exercises::add);
        return exercises;
    }
}
