package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.GeneratedPlan.GeneratedEntry;
import com.kinetiq.plan.domain.model.GeneratedPlan.GeneratedSession;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

/** A client with answers, their trainer and a small library, for the tests of drafting. */
final class DraftingFixture {

    static final String CLIENT_NAME = "Zdzisława Qwertyńska";
    static final String TRAINER_NAME = "Bogumił Asdfgowski";
    static final String NOTES = "SENTINEL-NOTES left knee";

    final AccountId client = AccountId.random();
    final AccountId trainer = AccountId.random();
    final List<Exercise> library = new ArrayList<>(IntStream.rangeClosed(1, 5)
            .mapToObj(number -> exercise("Exercise " + number))
            .toList());
    final InMemoryPlanRepository plans = new InMemoryPlanRepository();
    final InMemoryPlanDraftingRepository draftings = new InMemoryPlanDraftingRepository();

    AccountId clientsTrainer = trainer;
    boolean answered = true;

    final ViewQuizAnswersUseCase quiz = client -> answered ? Optional.of(answers()) : Optional.empty();

    final CurrentAccountUseCase accounts = new CurrentAccountUseCase() {
        @Override
        public Account find(AccountId id) {
            return id.equals(client)
                    ? account(id, CLIENT_NAME, Role.CLIENT, clientsTrainer)
                    : account(id, TRAINER_NAME, Role.TRAINER, null);
        }

        @Override
        public void changeTimeZone(AccountId id, ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Account update(AccountId id, UpdateAccountCommand command) {
            throw new UnsupportedOperationException();
        }
    };

    final ListExercisesUseCase exercises = new ListExercisesUseCase() {
        @Override
        public ExerciseListing list(ListExercisesQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Exercise> library(AccountId owner) {
            return List.copyOf(library);
        }
    };

    private static Exercise exercise(String name) {
        return new Exercise(
                ExerciseId.random(),
                null,
                new LocalizedText(name, ""),
                Category.LEGS,
                new LocalizedText("", ""),
                Measure.REPS,
                3,
                10,
                90,
                new LocalizedText("", ""));
    }

    private static Account account(AccountId id, String name, Role role, AccountId trainer) {
        return Account.restore(
                id,
                name,
                new Email(id.value() + "@example.com"),
                "hash",
                role,
                trainer,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.PL),
                true,
                null,
                null,
                null,
                null);
    }

    QuizSubmission answers() {
        return new QuizSubmission(
                QuizSubmissionId.random(),
                client,
                Goal.LOSE_FAT,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                30,
                178,
                new BigDecimal("80.0"),
                Set.of(Like.WEIGHTS),
                2,
                NOTES,
                Instant.parse("2026-11-05T10:00:00Z"));
    }

    /** An answer that passes every check: two sessions over keys {@code E1} to {@code E4}. */
    static GeneratedPlan validAnswer() {
        return new GeneratedPlan(List.of(session("Lower body", "MONDAY"), session("Upper body", "THURSDAY")));
    }

    /** An answer that fails the checks with an exercise outside the catalogue. */
    static GeneratedPlan answerWithUnknownExercise() {
        return new GeneratedPlan(List.of(session("Lower body", "MONDAY"), new GeneratedSession("Upper body", "THURSDAY", List.of(
                entry("E99"), entry("E2"), entry("E3"), entry("E4")))));
    }

    private static GeneratedSession session(String name, String day) {
        return new GeneratedSession(name, day, List.of(entry("E1"), entry("E2"), entry("E3"), entry("E4")));
    }

    private static GeneratedEntry entry(String key) {
        return new GeneratedEntry(key, 3, 10, null, 7, 90);
    }
}
