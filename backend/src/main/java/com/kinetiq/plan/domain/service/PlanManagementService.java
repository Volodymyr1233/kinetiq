package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.common.domain.output.NoticeBoard;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanAlreadyPublishedException;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.exception.PlanDraftingInProgressException;
import com.kinetiq.plan.domain.exception.PlanExerciseNotInLibraryException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanQuizNotSubmittedException;
import com.kinetiq.plan.domain.input.ManagePlanUseCase;
import com.kinetiq.plan.domain.input.PlanDetails;
import com.kinetiq.plan.domain.input.SavePlanCommand;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.plan.domain.output.PlanRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlanManagementService implements ManagePlanUseCase {

    private final ViewClientsUseCase clients;
    private final ListExercisesUseCase exercises;
    private final PlanRepository plans;
    private final NoticeBoard notices;
    private final Clock clock;

    @Override
    public PlanDetails get(AccountId trainer, AccountId client) {
        ClientSummary summary = clients.getClient(trainer, client);
        Plan plan = plans.findByClient(client).orElseThrow(PlanNotFoundException::new);
        return details(plan, summary, library(trainer));
    }

    // the lock makes the version comparison safe; two first saves are told apart by the client's unique constraint
    @Override
    public PlanDetails save(SavePlanCommand command) {
        ClientSummary summary = clients.getClient(command.trainer(), command.client());
        Map<ExerciseId, Exercise> library = library(command.trainer());
        requireInLibrary(command.sessions(), library);
        Optional<Plan> existing = plans.findByClientForUpdate(command.client());
        Plan edited = existing.map(stored -> edit(stored, command, summary))
                .orElseGet(() -> firstDraft(command, summary));
        Plan saved = plans.save(edited);
        // only a plan the client can see changed under them; a draft is the trainer's alone
        if (existing.filter(Plan::isPublished).isPresent()) {
            notices.raise(new Notice(command.client().value(), null, NoticeKind.PLAN_CHANGED));
        }
        return details(saved, summary, library);
    }

    // a second publish is refused as already published, not as stale, so the plan's state is checked first
    @Override
    public PlanDetails publish(AccountId trainer, AccountId client, long version) {
        ClientSummary summary = clients.getClient(trainer, client);
        Plan stored = plans.findByClientForUpdate(client).orElseThrow(PlanNotFoundException::new);
        if (stored.isPublished()) {
            throw new PlanAlreadyPublishedException();
        }
        requireNotDrafting(summary);
        requireSameVersion(stored, version);
        Plan published = plans.save(stored.publish(today(summary)));
        notices.raise(new Notice(client.value(), null, NoticeKind.PLAN_PUBLISHED));
        return details(published, summary, library(trainer));
    }

    private Plan edit(Plan stored, SavePlanCommand command, ClientSummary summary) {
        requireNotDrafting(summary);
        requireSameVersion(stored, command.version());
        return stored.edit(command.goal(), command.weeks(), command.sessions(), today(summary));
    }

    private static Plan firstDraft(SavePlanCommand command, ClientSummary summary) {
        // a version says this editor saw a plan, which is not there
        if (command.version() != null) {
            throw new PlanChangedException();
        }
        requireNotDrafting(summary);
        if (summary.getPlanState() == PlanState.NO_QUIZ) {
            throw new PlanQuizNotSubmittedException();
        }
        return Plan.draft(command.client(), command.goal(), command.weeks(), command.sessions());
    }

    // a draft being built again is about to be replaced, so a change to it now would be lost (research R3)
    private static void requireNotDrafting(ClientSummary summary) {
        if (summary.getPlanState() == PlanState.PLAN_DRAFTING) {
            throw new PlanDraftingInProgressException();
        }
    }

    private static void requireSameVersion(Plan stored, Long version) {
        if (!Objects.equals(stored.getVersion(), version)) {
            throw new PlanChangedException();
        }
    }

    private static void requireInLibrary(List<Session> sessions, Map<ExerciseId, Exercise> library) {
        boolean allThere = sessions.stream()
                .flatMap(session -> session.entries().stream())
                .allMatch(entry -> library.containsKey(entry.exercise()));
        if (!allThere) {
            throw new PlanExerciseNotInLibraryException();
        }
    }

    private Map<ExerciseId, Exercise> library(AccountId trainer) {
        return exercises.library(trainer).stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));
    }

    private PlanDetails details(Plan plan, ClientSummary summary, Map<ExerciseId, Exercise> library) {
        return new PlanDetails(plan, plan.currentWeek(today(summary)).orElse(null), library);
    }

    // weeks are Monday to Sunday in the client's time zone, not the server's or the trainer's
    private LocalDate today(ClientSummary summary) {
        return clock.instant().atZone(summary.getTimeZone()).toLocalDate();
    }
}
