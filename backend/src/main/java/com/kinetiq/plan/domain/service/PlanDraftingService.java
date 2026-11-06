package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.GeneratedPlanRejectedException;
import com.kinetiq.plan.domain.input.DraftInput;
import com.kinetiq.plan.domain.input.DraftPlanUseCase;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.plan.domain.output.PlanRepository;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlanDraftingService implements DraftPlanUseCase {

    private static final int WEEKS = 12;

    private final ViewQuizAnswersUseCase quiz;
    private final CurrentAccountUseCase accounts;
    private final ListExercisesUseCase exercises;
    private final PlanRepository plans;

    // answers point at the client, and a client at their trainer, so neither account can be missing once these are read
    @Override
    public Optional<DraftInput> input(AccountId client) {
        return quiz.answers(client).flatMap(answers -> {
            Account account = accounts.find(client);
            return account.getTrainerId()
                    .map(trainer -> new DraftInput(
                            answers,
                            trainer,
                            account.getName(),
                            accounts.find(trainer).getName(),
                            account.getSettings().language(),
                            exercises.library(trainer)));
        });
    }

    @Override
    public void storeBuilt(AccountId client, AccountId trainer, List<Session> sessions) {
        Optional<QuizSubmission> answers = storableAnswers(client);
        if (answers.isEmpty() || !accounts.find(client).getTrainerId().filter(trainer::equals).isPresent()) {
            return;
        }
        requireInLibrary(sessions, trainer);
        plans.save(Plan.built(client, answers.get().getGoal(), WEEKS, sessions));
    }

    @Override
    public void storeFailed(AccountId client) {
        storableAnswers(client)
                .filter(answers -> accounts.find(client).getTrainerId().isPresent())
                .ifPresent(answers -> plans.save(Plan.failed(client, answers.getGoal(), WEEKS)));
    }

    // a plan that is already there stays as it is
    private Optional<QuizSubmission> storableAnswers(AccountId client) {
        return plans.findByClient(client).isPresent() ? Optional.empty() : quiz.answers(client);
    }

    private void requireInLibrary(List<Session> sessions, AccountId trainer) {
        Set<ExerciseId> library =
                exercises.library(trainer).stream().map(Exercise::getId).collect(Collectors.toSet());
        boolean allThere = sessions.stream()
                .flatMap(session -> session.entries().stream())
                .allMatch(entry -> library.contains(entry.exercise()));
        if (!allThere) {
            throw new GeneratedPlanRejectedException(List.of(GeneratedPlanChecks.UNKNOWN_EXERCISE));
        }
    }
}
