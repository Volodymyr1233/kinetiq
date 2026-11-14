package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanAlreadyPublishedException;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.exception.PlanExerciseNotInLibraryException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanDraftingInProgressException;
import com.kinetiq.plan.domain.exception.PlanQuizNotSubmittedException;
import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import com.kinetiq.plan.domain.exception.PlanWeeksBeforeCurrentException;
import com.kinetiq.plan.domain.input.PlanDetails;
import com.kinetiq.plan.domain.input.SavePlanCommand;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanManagementServiceTest {

    private final List<Notice> notices = new ArrayList<>();

    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId OTHER_TRAINER = AccountId.random();
    private static final AccountId CLIENT = AccountId.random();
    private static final AccountId NO_QUIZ_CLIENT = AccountId.random();
    private static final AccountId DRAFTING_CLIENT = AccountId.random();
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    // Wednesday, in week 6 of a plan whose first week is Monday 28 September
    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");
    private static final LocalDate FIRST_WEEK = LocalDate.parse("2026-09-28");

    private static final Exercise SQUAT = exercise(null);
    private static final Exercise OWN = exercise(TRAINER);
    private static final Exercise FOREIGN = exercise(OTHER_TRAINER);

    private static Exercise exercise(AccountId owner) {
        return new Exercise(
                ExerciseId.random(),
                owner,
                new LocalizedText("Squat", "Przysiad"),
                Category.LEGS,
                new LocalizedText("", ""),
                Measure.REPS,
                3,
                10,
                90,
                new LocalizedText("", ""));
    }

    private final InMemoryPlanRepository plans = new InMemoryPlanRepository();

    private final ViewClientsUseCase clients = new ViewClientsUseCase() {
        private final Map<AccountId, ClientSummary> own = Map.of(
                CLIENT, new ClientSummary(CLIENT, "Ola", Goal.LOSE_FAT, PlanState.PLAN_TO_REVIEW, WARSAW),
                NO_QUIZ_CLIENT, new ClientSummary(NO_QUIZ_CLIENT, "Jan", null, PlanState.NO_QUIZ, WARSAW),
                DRAFTING_CLIENT, new ClientSummary(DRAFTING_CLIENT, "Ewa", Goal.LOSE_FAT, PlanState.PLAN_DRAFTING, WARSAW));

        @Override
        public List<ClientSummary> listClients(AccountId trainer) {
            return List.copyOf(own.values());
        }

        @Override
        public ClientSummary getClient(AccountId trainer, AccountId client) {
            if (!TRAINER.equals(trainer) || !own.containsKey(client)) {
                throw new ClientNotFoundException();
            }
            return own.get(client);
        }
    };

    private final ListExercisesUseCase library = new ListExercisesUseCase() {
        @Override
        public ExerciseListing list(ListExercisesQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Exercise> library(AccountId trainer) {
            return List.of(SQUAT, OWN);
        }
    };

    private final PlanManagementService service =
            new PlanManagementService(clients, library, plans, notices::add, Clock.fixed(NOW, ZoneOffset.UTC));

    private static List<Session> sessions(ExerciseId exercise) {
        ExerciseEntry entry = new ExerciseEntry(exercise, 3, 8, null, 8, 90);
        return List.of(
                new Session("Lower", DayOfWeek.MONDAY, List.of(entry)),
                new Session("Upper", DayOfWeek.THURSDAY, List.of(entry)));
    }

    private static SavePlanCommand save(AccountId client, Long version, int weeks, List<Session> sessions) {
        return new SavePlanCommand(TRAINER, client, version, Goal.BUILD_MUSCLE, weeks, sessions);
    }

    private Plan storedDraft() {
        return plans.save(Plan.draft(CLIENT, Goal.LOSE_FAT, 12, sessions(SQUAT.getId())));
    }

    private Plan storedPublished(int weeks) {
        return plans.save(
                new Plan(PlanId.random(), CLIENT, Goal.LOSE_FAT, weeks, FIRST_WEEK, sessions(SQUAT.getId()), 0));
    }

    @Test
    void shouldRefuseAFirstSaveAndStoreNothing_whileAPlanIsBeingDrafted() {
        assertThatThrownBy(() -> service.save(save(DRAFTING_CLIENT, null, 12, sessions(SQUAT.getId()))))
                .isInstanceOf(PlanDraftingInProgressException.class);

        assertThat(plans.byClient).doesNotContainKey(DRAFTING_CLIENT);
    }

    @Test
    void shouldAnswerNotFound_whenAnotherTrainerSavesForAClientBeingDrafted() {
        SavePlanCommand foreign =
                new SavePlanCommand(OTHER_TRAINER, DRAFTING_CLIENT, null, Goal.LOSE_FAT, 12, sessions(SQUAT.getId()));

        assertThatThrownBy(() -> service.save(foreign)).isInstanceOf(ClientNotFoundException.class);
    }

    // a draft being built again would lose a change made now, so the stored draft is refused like a first save
    @Test
    void shouldRefuseSavingAndPublishingAStoredDraft_whileItIsBeingBuiltAgain() {
        Plan draft = plans.save(Plan.draft(DRAFTING_CLIENT, Goal.LOSE_FAT, 12, sessions(SQUAT.getId())));

        assertThatThrownBy(() -> service.save(save(DRAFTING_CLIENT, draft.getVersion(), 12, sessions(SQUAT.getId()))))
                .isInstanceOf(PlanDraftingInProgressException.class);
        assertThatThrownBy(() -> service.publish(TRAINER, DRAFTING_CLIENT, draft.getVersion()))
                .isInstanceOf(PlanDraftingInProgressException.class);
        assertThat(plans.byClient.get(DRAFTING_CLIENT)).isSameAs(draft);
    }

    @Test
    void shouldClearTheMark_whenTheTrainerSavesAMarkedDraft() {
        Plan marked = plans.save(Plan.failed(CLIENT, Goal.LOSE_FAT, 12));

        PlanDetails saved =
                service.save(save(CLIENT, marked.getVersion(), 12, sessions(SQUAT.getId())));

        assertThat(saved.plan().getAutomaticDraft()).isEmpty();
        assertThat(plans.byClient.get(CLIENT).getAutomaticDraft()).isEmpty();
    }

    @Test
    void shouldClearTheMark_whenTheTrainerPublishesABuiltDraft() {
        Plan marked = plans.save(Plan.built(CLIENT, Goal.LOSE_FAT, 12, sessions(SQUAT.getId())));

        PlanDetails published = service.publish(TRAINER, CLIENT, marked.getVersion());

        assertThat(published.plan().isPublished()).isTrue();
        assertThat(published.plan().getAutomaticDraft()).isEmpty();
    }

    @Nested
    class Get {

        @Test
        void shouldGiveThePlanWithItsLibrary_withoutACurrentWeek_whenDraft() {
            Plan draft = storedDraft();

            PlanDetails details = service.get(TRAINER, CLIENT);

            assertThat(details.plan().getId()).isEqualTo(draft.getId());
            assertThat(details.currentWeek()).isNull();
            assertThat(details.library()).containsKeys(SQUAT.getId(), OWN.getId());
        }

        @Test
        void shouldGiveTheCurrentWeek_whenPublished() {
            storedPublished(16);

            assertThat(service.get(TRAINER, CLIENT).currentWeek()).isEqualTo(6);
        }

        @Test
        void shouldAnswerNoPlan_whenTheClientIsOwnAndHasNone() {
            assertThatThrownBy(() -> service.get(TRAINER, CLIENT)).isInstanceOf(PlanNotFoundException.class);
        }

        // a foreign client never gets the no-plan answer, so nothing leaks about who has a plan
        @Test
        void shouldAnswerClientNotFound_whenTheClientIsAnotherTrainersOrUnknown() {
            storedDraft();

            assertThatThrownBy(() -> service.get(OTHER_TRAINER, CLIENT)).isInstanceOf(ClientNotFoundException.class);
            assertThatThrownBy(() -> service.get(TRAINER, AccountId.random()))
                    .isInstanceOf(ClientNotFoundException.class);
        }
    }

    @Nested
    class Save {

        @Test
        void shouldCreateADraft_whenFirstSave() {
            PlanDetails details = service.save(save(CLIENT, null, 12, sessions(SQUAT.getId())));

            assertThat(details.plan().isPublished()).isFalse();
            assertThat(details.plan().getGoal()).isEqualTo(Goal.BUILD_MUSCLE);
            assertThat(plans.byClient.get(CLIENT).getWeeks()).isEqualTo(12);
        }

        @Test
        void shouldAllowAnIncompleteDraft() {
            assertThat(service.save(save(CLIENT, null, 12, List.of())).plan().getSessions()).isEmpty();
        }

        @Test
        void shouldRefuseAFirstSave_whenTheQuizIsNotSubmitted() {
            assertThatThrownBy(() -> service.save(save(NO_QUIZ_CLIENT, null, 12, List.of())))
                    .isInstanceOf(PlanQuizNotSubmittedException.class);
            assertThat(plans.byClient).isEmpty();
        }

        @Test
        void shouldReplaceTheDraftAndRaiseTheVersion_whenTheVersionMatches() {
            Plan draft = storedDraft();

            PlanDetails details = service.save(save(CLIENT, draft.getVersion(), 8, sessions(OWN.getId())));

            assertThat(details.plan().getId()).isEqualTo(draft.getId());
            assertThat(details.plan().getVersion()).isEqualTo(draft.getVersion() + 1);
            assertThat(plans.byClient.get(CLIENT).getWeeks()).isEqualTo(8);
        }

        @Test
        void shouldTellTheClientTheirPlanChanged_whenAPublishedPlanIsSaved() {
            Plan published = storedPublished(16);

            service.save(save(CLIENT, published.getVersion(), 16, sessions(OWN.getId())));

            assertThat(notices).containsExactly(new Notice(CLIENT.value(), null, NoticeKind.PLAN_CHANGED));
        }

        @Test
        void shouldTellNobody_whenADraftIsSaved() {
            Plan draft = storedDraft();

            service.save(save(CLIENT, draft.getVersion(), 8, sessions(OWN.getId())));

            assertThat(notices).isEmpty();
        }

        @Test
        void shouldRefuseAsChanged_whenTheVersionIsStale() {
            Plan draft = storedDraft();

            assertThatThrownBy(() -> service.save(save(CLIENT, draft.getVersion() + 1, 8, List.of())))
                    .isInstanceOf(PlanChangedException.class);
            assertThat(plans.byClient.get(CLIENT)).isSameAs(draft);
        }

        // another editor made the first save while this one was open
        @Test
        void shouldRefuseAsChanged_whenAPlanExistsAndNoVersionIsSent() {
            Plan draft = storedDraft();

            assertThatThrownBy(() -> service.save(save(CLIENT, null, 8, List.of())))
                    .isInstanceOf(PlanChangedException.class);
            assertThat(plans.byClient.get(CLIENT)).isSameAs(draft);
        }

        @Test
        void shouldRefuseAsChanged_whenAVersionIsSentAndThereIsNoPlan() {
            assertThatThrownBy(() -> service.save(save(CLIENT, 3L, 8, List.of())))
                    .isInstanceOf(PlanChangedException.class);
            assertThat(plans.byClient).isEmpty();
        }

        @Test
        void shouldKeepTheFirstWeekAndCurrentWeek_whenSavingAPublishedPlan() {
            Plan published = storedPublished(16);

            PlanDetails details = service.save(save(CLIENT, published.getVersion(), 16, sessions(OWN.getId())));

            assertThat(details.plan().getFirstWeek()).contains(FIRST_WEEK);
            assertThat(details.currentWeek()).isEqualTo(6);
            assertThat(details.plan().getSessions().getFirst().entries().getFirst().exercise())
                    .isEqualTo(OWN.getId());
        }

        @Test
        void shouldCheckAPublishedPlanIsComplete() {
            Plan published = storedPublished(16);

            assertThatThrownBy(() -> service.save(save(CLIENT, published.getVersion(), 16, List.of())))
                    .isInstanceOf(PlanRuleViolationException.class);
            assertThat(plans.byClient.get(CLIENT)).isSameAs(published);
        }

        @Test
        void shouldRefuseShorteningBelowTheCurrentWeek_inTheClientsTimeZone() {
            Plan published = storedPublished(16);

            assertThatThrownBy(() -> service.save(save(CLIENT, published.getVersion(), 5, sessions(SQUAT.getId()))))
                    .isInstanceOf(PlanWeeksBeforeCurrentException.class);
            assertThat(plans.byClient.get(CLIENT)).isSameAs(published);
        }

        @Test
        void shouldRefuseAndStoreNothing_whenTheClientIsAnotherTrainers() {
            Plan draft = storedDraft();
            SavePlanCommand command =
                    new SavePlanCommand(OTHER_TRAINER, CLIENT, draft.getVersion(), Goal.LOSE_FAT, 8, List.of());

            assertThatThrownBy(() -> service.save(command)).isInstanceOf(ClientNotFoundException.class);
            assertThat(plans.byClient.get(CLIENT)).isSameAs(draft);
        }

        // the same answer for both, so a save cannot be used to find out which exercises exist
        @Test
        void shouldRefuseAlike_whenAnExerciseIsAnotherTrainersOrUnknown() {
            assertThatThrownBy(() -> service.save(save(CLIENT, null, 12, sessions(FOREIGN.getId()))))
                    .isInstanceOf(PlanExerciseNotInLibraryException.class);
            assertThatThrownBy(() -> service.save(save(CLIENT, null, 12, sessions(ExerciseId.random()))))
                    .isInstanceOf(PlanExerciseNotInLibraryException.class);
            assertThat(plans.byClient).isEmpty();
        }
    }

    @Nested
    class Publish {

        private PlanManagementService at(String instant, ZoneId zone) {
            ViewClientsUseCase inZone = new ViewClientsUseCase() {
                @Override
                public List<ClientSummary> listClients(AccountId trainer) {
                    return List.of();
                }

                @Override
                public ClientSummary getClient(AccountId trainer, AccountId client) {
                    return new ClientSummary(CLIENT, "Ola", Goal.LOSE_FAT, PlanState.DRAFT, zone);
                }
            };
            return new PlanManagementService(
                    inZone, library, plans, notices::add, Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
        }

        @Test
        void shouldPublishTheDraft_startingWeekOneThisWeek() {
            Plan draft = storedDraft();

            PlanDetails details = service.publish(TRAINER, CLIENT, draft.getVersion());

            assertThat(details.plan().getFirstWeek()).contains(LocalDate.parse("2026-11-02"));
            assertThat(details.currentWeek()).isEqualTo(1);
            assertThat(details.plan().getVersion()).isEqualTo(draft.getVersion() + 1);
            assertThat(plans.byClient.get(CLIENT).isPublished()).isTrue();
        }

        @Test
        void shouldTellTheClientTheirPlanIsPublished() {
            Plan draft = storedDraft();

            service.publish(TRAINER, CLIENT, draft.getVersion());

            assertThat(notices).containsExactly(new Notice(CLIENT.value(), null, NoticeKind.PLAN_PUBLISHED));
        }

        @Test
        void shouldTellNobody_whenThePublishIsRefused() {
            Plan draft = storedDraft();

            assertThatThrownBy(() -> service.publish(TRAINER, CLIENT, draft.getVersion() + 5))
                    .isInstanceOf(PlanChangedException.class);

            assertThat(notices).isEmpty();
        }

        @Test
        void shouldFollowTheClientsTimeZone_whenItIsBehindOrAheadOfUtc() {
            Plan draft = storedDraft();
            ZoneId newYork = ZoneId.of("America/New_York");

            // Monday 00:30 in UTC is still Sunday evening in New York
            PlanDetails behind = at("2026-11-02T00:30:00Z", newYork).publish(TRAINER, CLIENT, draft.getVersion());
            assertThat(behind.plan().getFirstWeek()).contains(LocalDate.parse("2026-10-26"));

            plans.byClient.clear();
            Plan again = storedDraft();
            // Sunday 23:30 in UTC is already Monday in Warsaw
            PlanDetails ahead = at("2026-11-01T23:30:00Z", WARSAW).publish(TRAINER, CLIENT, again.getVersion());
            assertThat(ahead.plan().getFirstWeek()).contains(LocalDate.parse("2026-11-02"));
        }

        @Test
        void shouldRefuseAsChanged_whenTheVersionIsStale() {
            Plan draft = storedDraft();

            assertThatThrownBy(() -> service.publish(TRAINER, CLIENT, draft.getVersion() + 1))
                    .isInstanceOf(PlanChangedException.class);
            assertThat(plans.byClient.get(CLIENT).isPublished()).isFalse();
        }

        // the repeated request carries the version of before the first publish, and is told what really happened
        @Test
        void shouldRefuseAsAlreadyPublished_evenWithAnOldVersion() {
            Plan published = storedPublished(16);

            assertThatThrownBy(() -> service.publish(TRAINER, CLIENT, published.getVersion() - 1))
                    .isInstanceOf(PlanAlreadyPublishedException.class);
            assertThat(plans.byClient.get(CLIENT)).isSameAs(published);
        }

        @Test
        void shouldRefuse_whenTheDraftIsIncomplete() {
            Plan draft = plans.save(Plan.draft(CLIENT, Goal.LOSE_FAT, 12, List.of()));

            assertThatThrownBy(() -> service.publish(TRAINER, CLIENT, draft.getVersion()))
                    .isInstanceOf(PlanRuleViolationException.class);
            assertThat(plans.byClient.get(CLIENT).isPublished()).isFalse();
        }

        @Test
        void shouldAnswerNoPlan_whenThereIsNone() {
            assertThatThrownBy(() -> service.publish(TRAINER, CLIENT, 0)).isInstanceOf(PlanNotFoundException.class);
        }

        @Test
        void shouldRefuseAndChangeNothing_whenTheClientIsAnotherTrainers() {
            Plan draft = storedDraft();

            assertThatThrownBy(() -> service.publish(OTHER_TRAINER, CLIENT, draft.getVersion()))
                    .isInstanceOf(ClientNotFoundException.class);
            assertThat(plans.byClient.get(CLIENT)).isSameAs(draft);
        }
    }
}
